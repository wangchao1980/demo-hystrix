package com.redblue.controller;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.redblue.service.GirlService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class GirlController {

	@Autowired
	GirlService girlService;

	@GetMapping("/girlGirl")
	public String girlGirl() {
		Integer id = 1;
		int n = 10;
		for (int i = 0; i < 10; i++) {
			if (i == n / 2) {
				girlService.removeCache(id);
			}
			String result = girlService.getGirl(id);
			log.info("girlGirl: " + id + "-" + i + "-result-" + result);
		}

		return "OK";
	}

	@GetMapping("/getGirl")
	public String getGirl() {

		ThreadPoolTaskExecutor executor = getThreadPoolTaskExecutor();
		for (int i = 0; i < 100; i++) {
			int id = i;
			executor.execute(() -> {
				girlService.getGirlById(id);
			});
		}

		return "OK";
	}

	@GetMapping("/getOneGirl")
	public String getOneGirl() {

		Future<String> result1 = girlService.getOneGirlById(1);
		Future<String> result2 = girlService.getOneGirlById(2);
		Future<String> result3 = girlService.getOneGirlById(3);

		try {
			result1.get();
			result2.get();
			result3.get();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "OK";
	}

	@GetMapping("/tryGirl")
	public String tryGirl() {
		girlService.tryGirlById(1);
		return "OK";
	}

	@GetMapping("/limitGirl")
	public String limitGirl() {

		ThreadPoolTaskExecutor executor = getThreadPoolTaskExecutor();
		for (int i = 0; i < 100; i++) {
			int id = i;
			executor.execute(() -> {
				girlService.limitGirlById(id);
			});
		}

		return "OK";
	}

	@GetMapping("/threadPoolGirl")
	public String threadPoolGirl() {

		ThreadPoolTaskExecutor executor = getThreadPoolTaskExecutor();
		for (int i = 0; i < 100; i++) {
			int id = i;

			executor.execute(() -> {
				girlService.threadPoolGirlById(id);
			});
		}

		return "OK";
	}

	@GetMapping("/hello")
	public String hell(String name) {
		String s = "hello " + name + " !";
		log.info("s " + s + " " + new Date());
		// int i = 1 / 0;
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
	}
	
	@GetMapping("/wait")
	public String wait(String name) {
		String s = "hello " + name + " !";
		log.info("s " + s + " " + new Date());
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return s;
	}

	public ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		// 核心线程数
		executor.setCorePoolSize(3);

		// 最大线程数
		executor.setMaxPoolSize(100);

		// 队列最大长度
		executor.setQueueCapacity(100);

		// 线程池维护线程所允许的空闲时间
		executor.setKeepAliveSeconds(300);

		// 线程池对拒绝任务(无线程可用)的处理策略 ThreadPoolExecutor.CallerRunsPolicy策略
		// ,调用者的线程会执行该任务,如果执行器已关闭,则丢弃.
		// AbortPolicy:直接抛出java.util.concurrent.RejectedExecutionException异常
		// CallerRunsPolicy:若已达到待处理队列长度，将由主线程直接处理请求
		// DiscardOldestPolicy:抛弃旧的任务；会导致被丢弃的任务无法再次被执行
		// DiscardPolicy:抛弃当前任务；会导致被丢弃的任务无法再次被执行
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		// 初始化
		executor.initialize();

		return executor;
	}
}
