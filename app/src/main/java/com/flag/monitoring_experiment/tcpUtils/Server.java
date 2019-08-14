package com.flag.monitoring_experiment.tcpUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Server {
	
	private int port;
	private String ipAddress;
	private ServerSocket serversocket;
	
	public Server(int port, String ipAddress) throws UnknownHostException, IOException {
		this.port = port;
		this.ipAddress = ipAddress;
		serversocket = new ServerSocket(this.port,10, InetAddress.getByName(this.ipAddress));
	}
	
	public void service(){
		while(true){
			Socket socket = null;
			try {
				socket = serversocket.accept();
				System.out.println("New connection accept"+
						socket.getInetAddress()+":"+socket.getPort());
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				if(socket != null){
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}
		
	}

//	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
//		Server server = new Server();
//		//Thread.sleep(60000*10);
//		server.service();
//	}

}
