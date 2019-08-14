package com.flag.monitoring_experiment.tcpUtils;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Bmind on 2018/6/15.
 */

public class ThreadPool extends ThreadGroup {
    public String TAG= "ThreadPool";
    public static ExecutorService exec = Executors.newCachedThreadPool();//关闭任务所用

    private boolean isClosed = false;       //线程池是否关闭
    private LinkedList<Runnable> workQueue; //表示工作队列
    private static int threadPoolID;            //表示线程池ID
    private int threadID;                   //表示工作线程ID

    public ThreadPool(int poolSize) {
        super("ThreadPool"+(threadPoolID++));
        setDaemon(true);
        workQueue = new LinkedList<Runnable>();     //创建工作队列
        for(int i = 0;i<poolSize;i++){
            new WorkThread().start();               //创建并启动工作线程
        }
    }
    /*向工作队列中加入一个新任务，由工作线程去执行*/
    public synchronized  void execute(Runnable task){
        if (isClosed){                          //线程关闭则抛出IllegalStateException异常
            throw new IllegalStateException();
        }
        if (task != null){
            workQueue.add(task);
            notify();                       //唤醒正在getTask()方法中的等待任务的工作线程
        }
    }


    /*从工作队列中去取出一个任务，工作线程会调用此方法*/
    protected synchronized Runnable getTask() throws InterruptedException {
        while(workQueue.size()==0){
            if(isClosed)return null;
            wait();                     //如果工作队列中没有任务，就等待任务
        }
        return workQueue.removeFirst();
    }

    /*关闭线程池*/
    public synchronized void close(){
        if (!isClosed){
            isClosed = true;
            workQueue.clear();      //清空工作队列
            interrupt();            //中断所有的工作线程，该方法集成自ThreadGroup类
        }
    }

    /*等待工作你线程把所有任务执行完*/
    public void join(){
        synchronized (this){
            isClosed = true;
            notifyAll();            //唤醒还在getTask()方法中等待任务的工作线程
        }
    }


    /*内部类：工作线程*/
    private class WorkThread extends Thread {
        public WorkThread(){
            //加入到当前ThreadPool线程池中
            super(ThreadPool.this,"WorkThread-"+(threadID++));
        }
        private Future<?> f;
        private void stopTask(){         //关闭该任务
            f.cancel(true);
            task.closeSocket();         //关闭底层资源
            task = null;
        }
        IOBlockedRunnable task = null;
        @Override
        public void run() {
            while(!isInterrupted()){            //继承自Thread类，判断线程是否被中断
                try {
                    task = (IOBlockedRunnable) getTask();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
                //如果getTask（）返回null或者线程执行getTask()时被中断，则结束此线程
                if(task == null)return;
                try{
//                    task.run();
                    f = exec.submit(task);          //启动任务
                }catch (Throwable t){
                    t.printStackTrace();
                }
            }
        }
    }
}
