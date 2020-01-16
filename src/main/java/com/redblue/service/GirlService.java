package com.redblue.service;

import java.util.List;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

@Service
public interface GirlService {
	
	public String getGirl(Integer id);
	
	public void removeCache(Integer id);
	
	public String getGirlById(Integer id);
	
	public Future<String> getOneGirlById(Integer id);
	
	public String tryGirlById(Integer id);
	
	public String limitGirlById(Integer id);
	
	public String threadPoolGirlById(Integer id);
	
	public List<String> getGirlsByIds(List<Integer> ids);

}
