package client.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import protocol.*;
import client.view.AccessChat;
import client.view.ChatView;
import client.vo.*;

public class ScWithServer implements Runnable{
	ChatView cv;
	public Customer cus;											//클라이언트 정보를 담기 위한 vo객체
	Socket connection;
	private static ObjectOutputStream output;
	ObjectInputStream input;

	 public ScWithServer(String c_id) {
		 cus = new Customer(c_id);							//접속한 클라이언트의 c_id를 저장
		 try {
			connection = new Socket("70.12.115.73",6789);								//소켓연결 
			output = new ObjectOutputStream(connection.getOutputStream());		//스트림 연결
			output.flush();
			input = new ObjectInputStream(connection.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		 new Thread(this).start();
	 }
	 
	 //서버에게 프로토콜을 전송하는 메소드(주문, 채팅, 종료)
	 public static void sendProtocol(ClientProtocol obj){
		 try {
			 ClientProtocol proto = obj;
			output.writeObject(proto);
			output.flush();
			System.out.println("클라이언트 : 주문 프로토콜 전송 완료");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 //서버로부터 프로토콜을 받는 메소드
	 public void receiveProtocol(){
		 do{
			 try {
				ClientProtocol proto = (ClientProtocol)input.readObject();		//서버로부터 프로토콜을 받음
				
				//1.채팅 메시지를 받았을 시
				if(proto.getState() == proto.Chatting_Message){					
					cv = AccessChat.chat();
					cv.setVisible(true);
					String message = (String)proto.getData();
					cv.taChatAll.append("관리자 - : " + message + "\n");
				}
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("클라이언트입니다. whileChatting에 문제가있어요");
				System.exit(0);
			}
		 }while(true);
	 }
	@Override
	public void run() {
		try {
			
			System.out.println("클라이언트 입니다. 스트림이 연결되었네요");
			output.writeObject(cus.getC_id());											//서버에 접속자 c_id를 전송
			output.flush();
			receiveProtocol();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
