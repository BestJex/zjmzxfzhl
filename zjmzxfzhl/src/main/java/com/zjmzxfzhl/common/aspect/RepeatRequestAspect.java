package com.zjmzxfzhl.common.aspect;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.beanutils.PropertyUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.zjmzxfzhl.common.aspect.annotation.RepeatRequest;
import com.zjmzxfzhl.common.exception.BaseException;
import com.zjmzxfzhl.common.redlock.RedissonDistributedLocker;
import com.zjmzxfzhl.common.util.ShiroUtils;
import com.zjmzxfzhl.modules.app.common.AppSessionObject;
import com.zjmzxfzhl.modules.app.interceptor.AppLoginInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * 登录用户请求防重发处理
 * 
 * @author 庄金明
 *
 */
@Aspect
@Component
@Order(0) // 优先于RedissonLockAspect
@Slf4j
public class RepeatRequestAspect {
	@Autowired
	private RedissonDistributedLocker redissonDistributedLocker;

	// 管理端请求，若有多个modules则使用||运算符增加拦截controller方法，如下
	// @Pointcut("execution(* com.*..*.sys.controller.*.*(..)) || execution(* com.*..*.othermodule.controller.*.*(..))")
	// 或者使用拦截全部controller，然后排除个别非管理端的controller，如下
	// @Pointcut("execution(* com.*..*.controller.*.*(..)) && !execution(* com.*..*.app.controller.*.*(..))")
	@Pointcut("execution(* com.*..*.sys.controller.*.*(..))")
	private void controllerAspect() {
	}

	// APP端请求
	@Pointcut("execution(* com.*..*.app.controller.*.*(..))")
	private void controllerAspectForApp() {
	}

	@Around("controllerAspect()")
	public Object controllerAspectAround(ProceedingJoinPoint joinPoint) throws Throwable {
		String userId = ShiroUtils.getUserId();
		if (userId == null || userId.length() == 0) {
			return joinPoint.proceed();
		}
		return exec(joinPoint, userId);
	}

	@Around("controllerAspectForApp()")
	public Object controllerAspectForAppAround(ProceedingJoinPoint joinPoint) throws Throwable {
		AppSessionObject appSessionObject = (AppSessionObject) RequestContextHolder.getRequestAttributes().getAttribute(AppLoginInterceptor.APP_SESSION_OBJECT, RequestAttributes.SCOPE_REQUEST);
		if (appSessionObject == null || appSessionObject.getUserId() == null || appSessionObject.getUserId().length() == 0) {
			return joinPoint.proceed();
		}
		return exec(joinPoint, appSessionObject.getUserId());
	}

	private Object exec(ProceedingJoinPoint joinPoint, String userId) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, Throwable {
		int waitTime = 0, leaseTime = 30;
		String lockParams = "";
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		RepeatRequest repeatRequest = signature.getMethod().getAnnotation((RepeatRequest.class));
		if (repeatRequest != null) {// 如果controller包含该注解，则解析注解并封装参数lockParams
			Object[] params = joinPoint.getArgs();
			int[] lockIndexs = repeatRequest.lockIndexs();
			String[] fieldNames = repeatRequest.fieldNames();
			waitTime = repeatRequest.waitTime();
			leaseTime = repeatRequest.leaseTime();

			// 锁2个及2个以上参数时，fieldNames数量应与lockIndexs一致
			if (lockIndexs.length > 1 && lockIndexs.length != fieldNames.length) {
				throw new BaseException("lockIndexs与fieldNames数量不一致");
			}
			if (lockIndexs.length > 0) {
				StringBuffer lockParamsBuffer = new StringBuffer();
				for (int i = 0; i < lockIndexs.length; i++) {
					if (fieldNames.length == 0 || fieldNames[i] == null || fieldNames[i].length() == 0) {
						lockParamsBuffer.append("." + params[lockIndexs[i]]);
					} else {
						Object lockParamValue = PropertyUtils.getSimpleProperty(params[lockIndexs[i]], fieldNames[i]);
						lockParamsBuffer.append("." + lockParamValue);
					}
				}
				lockParams = lockParamsBuffer.toString();
			}
		}

		Object obj = null;
		// 取得锁的key
		StringBuffer keyBuffer = new StringBuffer();
		keyBuffer.append(signature.getDeclaringTypeName()).append(".").append(signature.getName()).append(".").append(userId).append(lockParams);
		String key = keyBuffer.toString();
		RLock rlock = redissonDistributedLocker.getLock(key);
		boolean isSuccess = redissonDistributedLocker.tryLock(rlock, waitTime, leaseTime, TimeUnit.SECONDS);
		if (isSuccess) {
			log.info("取到锁[" + key + "]");
			try {
				obj = joinPoint.proceed();
			} finally {
				log.info("释放锁[" + key + "]");
				redissonDistributedLocker.unlock(rlock);
			}
		} else {
			throw new BaseException(repeatRequest == null ? "交易未执行完毕，请勿重复提交" : repeatRequest.msg());
		}
		return obj;
	}
}
