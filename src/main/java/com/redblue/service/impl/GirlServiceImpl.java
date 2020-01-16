package com.redblue.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.cache.annotation.CacheRemove;
import com.netflix.hystrix.contrib.javanica.cache.annotation.CacheResult;
import com.netflix.hystrix.contrib.javanica.conf.HystrixPropertiesManager;
import com.redblue.service.GirlService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GirlServiceImpl implements GirlService {

	@Autowired
	RestTemplate restTemplate;

	// 测试缓存
	@CacheResult(cacheKeyMethod = "getKeyMethod") // 该注解用来标记请求命令返回的结果应该被缓存，它必须与@HystrixCommand注解结合使用，属性：cacheKeyMethod
	@HystrixCommand(commandKey = "getGirl")
	// @CacheKey
	// 该注解用来在请求命令的参数上标记，使其作为cacheKey，如果没有使用此注解则会使用所有参数列表中的参数作为cacheKey，属性：value
	public String getGirl(Integer id) {
		log.info("getGirl: " + id);
		Random random = new java.util.Random();
		int n = random.nextInt(5);
		return "girl-" + id + "-" + n;
	}

	// 生成cacheKey
	public String getKeyMethod(Integer id) {
		return "girl" + id;
	}

	@CacheRemove(commandKey = "getGirl", cacheKeyMethod = "getKeyMethod") // 该注解用来让请求命令的缓存失效，失效的缓存根据commandKey进行查找，属性：commandKey,cacheKeyMethod
	@HystrixCommand
	public void removeCache(Integer id) {
		log.info("removeCache id:{}", id);
	}

	// 熔断机制 相当于一个强化的服务降级。 服务降级是只要远程服务出错，立刻返回fallback结果。
	// 熔断是收集一定时间内的错误比例，如果达到一定的错误率。则启动熔断，返回fallback结果。 间隔一定时间会将请求再次发送给application
	// service进行重试。如果重试成功，熔断关闭。 如果重试失败，熔断持续开启，并返回fallback数据。
	//
	// # Circuit Breaker相关的属性
	// # 是否开启熔断器。默认true
	// hystrix.command.default.circuitBreaker.enabled=true
	// # 一个rolling window内最小的请求数。如果设为20，那么当一个rolling window的时间内（比如说1个rolling
	// window是10毫秒）收到19个请求
	// # 即使19个请求都失败，也不会触发circuit break。默认20
	// hystrix.command.default.circuitBreaker.requestVolumeThreshold=20
	// # 触发短路的时间值，当该值设为5000时，则当触发circuit
	// break后的5000毫秒内都会拒绝远程服务调用，也就是5000毫秒后才会重试远程服务调用。默认5000
	// hystrix.command.default.circuitBreaker.sleepWindowInMilliseconds=5000
	// # 错误比率阀值，如果错误率>=该值，circuit会被打开，并短路所有请求触发fallback。默认50
	// hystrix.command.default.circuitBreaker.errorThresholdPercentage=50
	// # 强制打开熔断器
	// hystrix.command.default.circuitBreaker.forceOpen=false
	// # 强制关闭熔断器
	// hystrix.command.default.circuitBreaker.forceClosed=false

	// 测试断路器
	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = HystrixPropertiesManager.CIRCUIT_BREAKER_ENABLED, value = "true"),
			// 默认20个;10ms内请求数大于20个时就启动熔断器，当请求符合熔断条件时将触发getFallback()。
			@HystrixProperty(name = HystrixPropertiesManager.CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD, value = "10"),
			// 请求错误率大于50%时就熔断，然后for循环发起请求，当请求符合熔断条件时将触发getFallback()。
			@HystrixProperty(name = HystrixPropertiesManager.CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE, value = "50"),
			// 默认5秒;熔断多少秒后去尝试请求
			@HystrixProperty(name = HystrixPropertiesManager.CIRCUIT_BREAKER_SLEEP_WINDOW_IN_MILLISECONDS, value = "5000"), }, fallbackMethod = "getDefaultGirlById")
	@Override
	public String getGirlById(Integer id) {

		Random random = new java.util.Random();
		int n = random.nextInt(5);
		log.info("n: " + n);
		if (n == 4) {
			// throw new RuntimeException();
		} else if (n == 3) {
			;
		}

		String result = "girl-" + id;
		log.info("getGirlById: " + result);

		return result;
	}

	public String getDefaultGirlById(Integer id) {
		String result = "defualt-girl-" + id;
		log.info("getDefaultGirlById: " + result);
		// e.printStackTrace();

		return result;
	}

	// 测试retry yml配置文件配置内容，另外restTemplate设置超时时间
	// spring.cloud.loadbalancer.retry.enabled：该参数用来开启重试机制，它默认是关闭的。
	// hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds：断路器的超时时间需要大于Ribbon的超时时间，不然不会触发重试。
	// demo-hystrix.ribbon.ConnectTimeout：请求连接时间。
	// demo-hystrix.ribbon.ReadTimeout：请求处理时间。
	// demo-hystrix.ribbon.OkToRetryOnAllOperations：对所有操作都进行重试。
	// demo-hystrix.ribbon.MaxAutoRetriesNextServer：切换实例的重试次数。
	// demo-hystrix.ribbon.MaxAutoRetries：对当前实例的重试次数。
	@Override
	public String tryGirlById(Integer id) {
		log.info("tryGirlById: " + id);
		restTemplate.getForObject("http://demo-hystrix/hello?name={1}", String.class, id);
		return String.valueOf(id);
	}

	// 请求合并
	@HystrixCollapser(batchMethod = "getGirlsByIds", collapserProperties = {
			@HystrixProperty(name = HystrixPropertiesManager.TIMER_DELAY_IN_MILLISECONDS, value = "20"), // 请求时间间隔在20ms之内的请求会被合并为一个请求,默认为10ms
			@HystrixProperty(name = HystrixPropertiesManager.MAX_REQUESTS_IN_BATCH, value = "100"), }) // 设置触发批处理执行之前，在批处理中允许的最大请求数。
	public Future<String> getOneGirlById(Integer id) {
		log.info("getGirlById: " + id);
		return null;
	}

	@HystrixCommand
	public List<String> getGirlsByIds(List<Integer> ids) {
		List<String> results = new ArrayList<String>();
		log.info("getGirlsByIds: " + ids.toString());

		if (ids != null) {
			for (Integer id : ids) {
				results.add(String.valueOf(id));
			}
		}

		log.info("getGirlsByIds-results: " + results.toString());

		return results;
	}

	// public static final String EXECUTION_ISOLATION_STRATEGY =
	// "execution.isolation.strategy";
	// public static final String EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS
	// = "execution.isolation.thread.timeoutInMilliseconds";
	// public static final String EXECUTION_TIMEOUT_ENABLED =
	// "execution.timeout.enabled";
	// public static final String EXECUTION_ISOLATION_THREAD_INTERRUPT_ON_TIMEOUT =
	// "execution.isolation.thread.interruptOnTimeout";
	// public static final String
	// EXECUTION_ISOLATION_SEMAPHORE_MAX_CONCURRENT_REQUESTS =
	// "execution.isolation.semaphore.maxConcurrentRequests";
	// 限制并发
	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = HystrixPropertiesManager.EXECUTION_ISOLATION_STRATEGY, value = "SEMAPHORE"),
			// 请求处理最大并发数，如果并发数达到该设置值，请求会被拒绝和抛出异常并且fallback不会被调用。默认10。
			@HystrixProperty(name = HystrixPropertiesManager.EXECUTION_ISOLATION_SEMAPHORE_MAX_CONCURRENT_REQUESTS, value = "1"), }, fallbackMethod = "getGirlOverLimit")
	@Override
	public String limitGirlById(Integer id) {
		log.info("limitGirlById: " + id);
		return String.valueOf(id);
	}

	public String getGirlOverLimit(Integer id) {
		String result = "over-limit-girl-" + id;
		log.info("getGirlOverLimit: " + result);
		// e.printStackTrace();

		return result;
	}

	// # Execution相关的属性的配置：
	// # 隔离策略，默认是Thread, 可选Thread｜Semaphore
	// hystrix.command.default.execution.isolation.strategy=THREAD
	// #命令执行超时时间，默认1000ms，只在线程池隔离中有效。
	// hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds=1000
	// # 执行是否启用超时，默认启用true，只在线程池隔离中有效。
	// hystrix.command.default.execution.timeout.enabled=true
	// # 发生超时是是否中断，默认true，只在线程池隔离中有效。
	// hystrix.command.default.execution.isolation.thread.interruptOnTimeout=true
	// #
	// 最大并发请求数，默认10，该参数当使用ExecutionIsolationStrategy.SEMAPHORE策略时才有效。如果达到最大并发请求数，请求会被拒绝。
	// #
	// 理论上选择semaphore的原则和选择thread一致，但选用semaphore时每次执行的单元要比较小且执行速度快（ms级别），否则的话应该用thread。
	// # semaphore应该占整个容器（tomcat）的线程池的一小部分。
	// hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests=10
	// # 如果并发数达到该设置值，请求会被拒绝和抛出异常并且fallback不会被调用。默认10。
	// #
	// 只在信号量隔离策略中有效，建议设置大一些，这样并发数达到execution最大请求数时，会直接调用fallback，而并发数达到fallback最大请求数时会被拒绝和抛出异常。
	// hystrix.command.default.fallback.isolation.semaphore.maxConcurrentRequests=10

	// # ThreadPool 相关参数
	// # 并发执行的最大线程数，默认10
	// hystrix.threadpool.default.coreSize=10
	// # BlockingQueue的最大队列数，当设为-1，会使用SynchronousQueue，值为正时使用LinkedBlcokingQueue。
	// # 该设置只会在初始化时有效，之后不能修改threadpool的queue size，除非reinitialising thread
	// executor。默认-1。
	// hystrix.threadpool.default.maxQueueSize=-1
	// # 即使maxQueueSize没有达到，达到queueSizeRejectionThreshold该值后，请求也会被拒绝。
	// hystrix.threadpool.default.queueSizeRejectionThreshold=20
	// # 线程存活时间，单位是分钟。默认值为1。
	// hystrix.threadpool.default.keepAliveTimeMinutes=1
	// # Fallback相关的属性
	// # 当执行失败或者请求被拒绝，是否会尝试调用fallback方法 。默认true
	// hystrix.command.default.fallback.enabled=true

	// 隔离
	// public static final String MAX_QUEUE_SIZE = "maxQueueSize";
	// public static final String CORE_SIZE = "coreSize";
	// public static final String KEEP_ALIVE_TIME_MINUTES = "keepAliveTimeMinutes";
	// public static final String QUEUE_SIZE_REJECTION_THRESHOLD =
	// "queueSizeRejectionThreshold";
	// public static final String METRICS_ROLLING_STATS_NUM_BUCKETS =
	// "metrics.rollingStats.numBuckets";
	// public static final String METRICS_ROLLING_STATS_TIME_IN_MILLISECONDS =
	// "metrics.rollingStats.timeInMilliseconds";
	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = HystrixPropertiesManager.EXECUTION_ISOLATION_STRATEGY, value = "THREAD") }, threadPoolProperties = {
					// 并发执行的最大线程数，默认10
					@HystrixProperty(name = HystrixPropertiesManager.CORE_SIZE, value = "10"),
					// 最大队列数默认-1
					@HystrixProperty(name = HystrixPropertiesManager.MAX_QUEUE_SIZE, value = "100"),
					// 线程存活时间，单位是分钟。默认值为1。
					@HystrixProperty(name = HystrixPropertiesManager.KEEP_ALIVE_TIME_MINUTES, value = "1"),
					// 即使maxQueueSize没有达到，达到queueSizeRejectionThreshold该值后，请求也会被拒绝。
					@HystrixProperty(name = HystrixPropertiesManager.QUEUE_SIZE_REJECTION_THRESHOLD, value = "100") }, threadPoolKey = "threadPoolGirl", groupKey = "threadPoolGirlById", fallbackMethod = "getGirlthreadPool")
	@Override
	public String threadPoolGirlById(Integer id) {
		log.info("threadPoolGirlById: " + id);
		try {
			restTemplate.getForObject("http://demo-hystrix/wait", String.class);
			//Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return String.valueOf(id);
	}

	public String getGirlthreadPool(Integer id) {
		String result = "over-limit-girl-" + id;
		log.info("getGirlOverLimit: " + result);
		// e.printStackTrace();

		return result;
	}

}

//# hystrix.command.default和hystrix.threadpool.default中的default为默认CommandKey，CommandKey默认值为服务方法名。
//# 在properties配置中配置格式混乱，如果需要为每个方法设置不同的容错规则，建议使用yml文件配置。
//# Command Properties
