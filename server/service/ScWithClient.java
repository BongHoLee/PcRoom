package server.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.sql.SQLException;
import java.util.ArrayList;

import server.model.*;
import server.view.*;
import server.vo.*;
import protocol.ClientProtocol;
import protocol.Order;

//클라이언트가 접속시 호출되어서 실행되는 스레드
public class ScWithClient implements Runnable {
	public static PcInfo pcinfo;
	Runnable updateTh;
	Thread upth;
	PcInfoModel pcinfomodel;
	SCustomer cus;
	UsePc usepc;
	UsePcModel usepcmodel;
	ChatView cv;
	

	Socket connection; // 클라이언트와 연결시 생성되는 소켓
	PanSeat pan;
	ObjectOutputStream output; // 소켓 통신시 메시지를 전달할 스트림 객체
	ObjectInputStream input; // 소켓 통신시 메시지를 전달받을 스트림 객체

	public ScWithClient(Socket socket, PanSeat pan) throws IOException, SQLException {
		this.cus = new SCustomer(); // Customer의 정보를 담는 VO 객체
		this.pcinfo = new PcInfo(); // PC의 정보를 담는 VO 객체
		this.pcinfomodel = new PcInfoModel(); // PC테이블의 데이터를 처리할 모델
		this.usepc = new UsePc(); // USE_PC의 정보를 담는 VO 객체
		this.usepcmodel = new UsePcModel(); // USE_PC의 데이터를 처리할 모델

		this.pan = pan; // 넘겨받은 좌석의 객체
		this.connection = socket;

		new Thread(this).start(); // 클라이언트와 연결된 소켓을 넘겨받고 run
	}

	// Stream을 설정
	public void setUpStreams() throws IOException {
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection.getInputStream());
		System.out.println("클라이언트와 스트림이 연결되었습니다.");
	}

	// 클라이언트의 C_ID를 받은 후 PcInfoModel을 호출해서 PC테이블 접근
	public void callPcUp() throws ClassNotFoundException, IOException {
		String c_id = (String) input.readObject(); // 클라이언트로부터 C_id를 받음
		pcinfo.setPc_no(pcinfomodel.selectPcNo(connection.getInetAddress())); // IP를	 이용해 PC_NO를 얻어옴		
		pcinfo.setPc_ip(connection.getInetAddress().toString()); // pcinfo 객체에 pc_no, pc_ip 저장
		cus.setC_id(c_id); // 현재 사용중인 C_id 저장
		
		//채팅뷰를 만들어서 hashMap에 넣어준다.
		cv = new ChatView(connection, output);
		ServerSc.chatMap.put(pcinfo.getPc_no(), cv);
		this.cv = ServerSc.chatMap.get(pcinfo.getPc_no());
	}

	// PC테이블의 FLAG를 업데이트 하기 위한 메소드
	public void updatePcFlag() {
		pcinfomodel.updatePcFlag(pcinfo);
		System.out.println("FLAG 갱신 완료");
	}

	// USE_PC 테이블에 입력(아직 나머지는 null)
	public void insertUsePc() {
		usepc.setC_id(cus.getC_id());
		usepc.setPc_no(pcinfo.getPc_no());
		usepc.setM_id("manager");
		usepc.setUsetime(0);
		usepc.setUse_flag(0);
		usepcmodel.insertByVo(usepc);
		
	}

	
	//좌석의 사용 정보를 갱신하는 메소드. 스레드를 실행한다.
	public void updateSeat() throws Exception {
		pan.setSeatInfo(1);
		System.out.println("좌석 바뀌었는지 확인");
		//Runnable r = new SeatUpdateTh(pan, usepc);
		updateTh = new SeatUpdateTh(pan, usepc);
		upth = new Thread(updateTh);
		upth.start();
		
	}
	
	
	// 클라이언트가 보낸 프로토콜을 전송받는다.
	public void receiveProtocol() {
		while (true) {
			try {
				//클라이언트가 보낸 프로토콜을 받도록 대기
				ClientProtocol protocol = (ClientProtocol) input.readObject();
				
				//1. 주문시 order_pro 테이블을 갱신하는 OrderTh 스레드 호출 및 실행
				if(protocol.getState() == protocol.Order_Send){
					ArrayList<Order> list = (ArrayList)protocol.getData();
					Runnable r = new OrderTh(list, pcinfo.getPc_no());					
				}
				//2. 채팅 메시지일시
				if(protocol.getState() == protocol.Chatting_Message){
					//ChatView cv1 = ServerSc.chatMap.get(usepc.getPc_no());
					//cv = AccessChat.chat();
					cv.setVisible(true);
					String message = (String)protocol.getData();
					cv.taChatAll.append("고객  -: " + message + " \n");
					
				}
				
				//3. 종료 메시지일시.
				if(protocol.getState() == protocol.EXIT){
					ClientProtocol proto = new ClientProtocol();
					proto.setState(proto.EXIT);
					sendProtocol(proto);
					break;
				}
			System.out.println("서버입니다. 클라이언트가 보낸 프로토콜을 받았어요 "+ protocol.getState());
				//System.out.println("서버입니다. 클라이언트가 보낸 프로토콜입니다 : + " +protocol);
			} catch (Exception  e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.out.println("서버인데요.. 클라이언트가 보낸 프로토콜을 못받았어요..");
				System.exit(0);
			}
		}
	}
	
	//클라이언트에게 프로토콜을 전송한다.
	public void sendProtocol(ClientProtocol pro){
		try {
			output.writeObject(pro);
			output.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage() + " 클라이언트에게 메시지 보내는 도중 오류");
		}
	}
	
	
	//클라이언트 연결 종료시 실행되는 메소드
	public void closeSoc() {
		try {
			System.out.println(usepc.getPc_no() + "번 PC 종료");
			pan.setSeatInfo(0);
			usepcmodel.calPc(usepc);											//use_pc 테이블 관련 갱신
			upth.interrupt();  													//좌석갱신 스레드 종료
			pan.label[1].setText("");
			pan.label[2].setText("");
			
			usepcmodel.updateFlag(usepc);
			String message = usepc.getPc_no()+"번 PC가 종료되었습니다. 총 금액 : " + usepc.getUsetotal();
			new MyDialog2(null, message);
			output.close();
			input.close();
			connection.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void welCom(){
		new MyDialog2(null, usepc.getPc_no()+"번 자리에 "+usepc.getC_id()+" 님이 접속하셨습니다.");
	}

	@Override
	// 클라이언트와 연결이 되었으니 이제 통신을 하면 된다.
	// 먼저 클라이언트로부터 C_ID를 받아서
	public void run() {

		// System.out.println(connection.getInetAddress());
		try {
			setUpStreams();
			callPcUp(); // PC_NO을 받아오기 위해서 호출
			updatePcFlag(); // PC테이블의 Flag를 1로 갱신
			insertUsePc(); // USE_PC 테이블에 INSERT
			updateSeat();		//좌석 정보 갱신
			welCom(); 			//환영메시지
			receiveProtocol();
			System.out.println("종료가 되었습니다.");
		}  catch (IOException e) {
			System.out.println("클라이언트와 스트림 연결 에러");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			closeSoc();					//최종적으로 소켓을 종료
			ServerSc.chatMap.remove(usepc.getC_id());
			System.out.println("hashMap에서 chatView가 제거되었습니다.");
			System.out.println("서버소켓이 종료되었습니다.");
		}

	}

}
