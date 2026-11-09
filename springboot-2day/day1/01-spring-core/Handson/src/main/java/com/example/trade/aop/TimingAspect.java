package com.example.trade.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect 
@Component 
public class TimingAspect {
    private static final Logger log=LoggerFactory.getLogger(TimingAspect.class);
    
    @Around ("execution(* com.example.trade.service.*.*(..))")
    public Object time(ProceedingJoinPoint pjp) throws Throwable{
        long start=System.nanoTime();
        try{
            return pjp.proceed();
        } finally{
            log.info("{} took us {}", pjp.getSignature().toShortString(),(System.nanoTime()-start)/1000);
        }
    }    
}
