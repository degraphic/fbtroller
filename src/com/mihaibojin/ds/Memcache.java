package com.mihaibojin.ds;

import java.util.logging.Level;

import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class Memcache {
	private static Memcache instance;
	private MemcacheService cache;
	
	private final Level LOGLEVEL = Level.INFO;
	
	protected Memcache()
	{
		// remove access_token from memcache, preventing jobs to run
		cache = MemcacheServiceFactory.getMemcacheService();
		cache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(LOGLEVEL));
	}
	
	/**
	 * Singleton constructor
	 * 
	 * @return Memcache
	 */
	public static Memcache getInstance()
	{
		if (null == instance) {
			instance = new Memcache();
		}
		
		return instance;
	}
	
	/**
	 * Set key, value to Memcache
	 * @param key
	 */
	public void put(String key, byte[] value)
	{
	    cache.put(key, value);
	}
	
	/**
	 * Load key from Memcache
	 * @param key
	 */
	public byte[] get(String key)
	{
	    return (byte[])cache.get(key);
	}
	
	
	/**
	 * Delete key from Memcache
	 * @param key
	 */
	public void delete(String key)
	{
	    cache.delete(key);
	}
	
	
}
