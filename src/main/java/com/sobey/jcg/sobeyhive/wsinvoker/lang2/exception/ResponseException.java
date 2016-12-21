package com.sobey.jcg.sobeyhive.wsinvoker.lang2.exception;

public class ResponseException extends Exception
{
	public ResponseException(String fault){
		super(fault);
	}
	
	public ResponseException(Throwable e){
		super(e);
	}
}
