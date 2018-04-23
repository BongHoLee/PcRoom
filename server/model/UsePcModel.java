package server.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import server.vo.UsePc;

//접속한 클라이언트의 정보를 받아서 USE_PC 테이블을 조작하는 모델.
//넘겨받는 인자는 주로 UsePc 객체를 넘겨받아서 갱신
public class UsePcModel {
	private Connection con;
	
	//DB에 연동시킴
	public UsePcModel() throws SQLException {
		con = DBCon.getConnection();
	}
	
	//종료시 해당 use_no의 pc가 주문한 메뉴의 총 합을 저장.
	public void calPc(UsePc uc) throws SQLException{
		
		//use_charge의 값을 uc에 저장
		String sql2 = "SELECT use_charge FROM use_pc WHERE use_no=?";
		PreparedStatement ps2 = con.prepareStatement(sql2);
		ps2.setInt(1, uc.getUse_no());
		ResultSet rs2 = ps2.executeQuery();
		if(rs2.next()){
			uc.setUsecharge(rs2.getInt("USE_CHARGE"));
		}
		rs2.close();
		ps2.close();

		//use_no에 해당하는 product 가격을 모두 구해와서 use_charge와 합한 값을 uc에 저장
		String sql1 = "SELECT SUM(product.pro_price) FROM product, order_pro, use_pc WHERE order_pro.order_flag=0 AND product.pro_no = order_pro.pro_no AND order_pro.use_no=?";
		PreparedStatement ps = con.prepareStatement(sql1);
		ps.setInt(1, uc.getUse_no());
		ResultSet rs = ps.executeQuery();
		if(rs.next()){
			uc.setUsetotal(uc.getUsecharge() + rs.getInt(1));
		}
		rs.close();
		ps.close();
		
		//use_total의 값을 갱신
		String sql3 = "UPDATE use_pc SET use_total=? WHERE use_no=?";
		PreparedStatement ps3 = con.prepareStatement(sql3);
		ps3.setInt(1, uc.getUsetotal());
		ps3.setInt(2, uc.getUse_no());
		ps3.executeUpdate();
		ps3.close();
		
		//String sql4 = "DELETE FROM use_pc WHERE use_no=?";
	}
	
	public void insertByVo(UsePc usepc){
		String sql = "INSERT INTO USE_PC(use_no, c_id, pc_no, m_id, use_time, use_charge, use_total) VALUES(usepc_seq.nextval, ?, ?, ?, 0, 0, 0)";
		String sql2 = "SELECT use_no FROM USE_PC WHERE c_id=?";
		try {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, usepc.getC_id());
			ps.setInt(2, usepc.getPc_no());
			ps.setString(3, usepc.getM_id());
			System.out.println("cid : " + usepc.getC_id());
			System.out.println("pc_no : " + usepc.getPc_no());
			System.out.println("m_id : " + usepc.getM_id());
			ps.executeUpdate();
			ps.close();
			
			PreparedStatement ps2 = con.prepareStatement(sql2);
			ps2.setString(1, usepc.getC_id());
			ResultSet rs = ps2.executeQuery();
			if(rs.next()){
				usepc.setUse_no(rs.getInt("USE_NO"));
			}
			rs.close();
			ps2.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	

}
