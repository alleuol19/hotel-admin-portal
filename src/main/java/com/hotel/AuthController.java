package com.hotel;


import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;



@Controller
public class AuthController {

	static String url = "jdbc:mysql://mysql.railway.internal:3306/railway";
	static String dbUser = "root";
	static String dbPass = "qIOlaWsFbfipeyehbTLbiscGTAvfJyNv";
	
	
	@GetMapping("/")
	public String home() {
	    return "redirect:/admin-login";
	}

	
	private List<String> getAvailableRooms(Connection conn, String roomType) {

	    List<String> rooms = new ArrayList<>();

	    int start = 101;
	    int end = 110;

	    if (roomType.equals("Economy Room")) {

	        start = 101;
	        end = 110;

	    }
	    else if (roomType.equals("Normal Room")) {

	        start = 201;
	        end = 210;

	    }
	    else if (roomType.equals("VIP Suite")) {

	        start = 301;
	        end = 310;

	    }

	    try {

	        for (int i = start; i <= end; i++) {

	            String roomNumber = "R" + i;

	            String sql =
	                    "SELECT COUNT(*) FROM booking " +
	                    "WHERE room_number = ? " +
	                    "AND status = 'SUCCESSFUL'";

	            PreparedStatement stmt =
	                    conn.prepareStatement(sql);

	            stmt.setString(1, roomNumber);

	            ResultSet rs = stmt.executeQuery();

	            if (rs.next() && rs.getInt(1) == 0) {

	                rooms.add(roomNumber);

	            }

	            rs.close();
	            stmt.close();
	        }

	    } catch (Exception e) {

	        e.printStackTrace();

	    }

	    return rooms;
	}
	@GetMapping("/admin-dashboard")
	public String adminDashboard(
	        HttpSession session,
	        Model model
	) {

	    if(session.getAttribute("admin") == null) {
	        return "redirect:/admin-login";
	    }

	    List<Map<String, Object>> pendingBookings = new ArrayList<>();
	    List<Map<String, Object>> successfulBookings = new ArrayList<>();
	    List<Map<String, Object>> cancelledBookings = new ArrayList<>();
	    List<Map<String, Object>> users = new ArrayList<>();
	    List<Map<String, Object>> feedbackList = new ArrayList<>();

	    try {

	        Connection conn = DriverManager.getConnection(url, dbUser, dbPass);

	        String sql = "SELECT * FROM booking ORDER BY id DESC";

	        PreparedStatement stmt = conn.prepareStatement(sql);

	        ResultSet rs = stmt.executeQuery();

	        while(rs.next()) {

	            Map<String, Object> booking = new HashMap<>();

	            booking.put("id", rs.getInt("id"));
	            booking.put("fullName", rs.getString("full_name"));
	            booking.put("contactNumber", rs.getString("contact_number"));
	            booking.put("email", rs.getString("email"));
	            booking.put("roomType", rs.getString("room_type"));
	            booking.put("checkInDate", rs.getString("check_in_date"));
	            booking.put("checkOutDate", rs.getString("check_out_date"));
	            booking.put("guests", rs.getInt("guests"));
	            booking.put("price", rs.getDouble("price"));
	            booking.put("status", rs.getString("status"));
	            booking.put("paymentMethod", rs.getString("payment_method"));
	            booking.put("roomNumber", rs.getString("room_number"));

	            String status = rs.getString("status");

	            if(status.equals("PENDING")) {

	                List<String> availableRooms =
	                        getAvailableRooms(
	                                conn,
	                                rs.getString("room_type")
	                        );

	                booking.put("availableRooms", availableRooms);

	                pendingBookings.add(booking);
	            }
	            else if(status.equals("SUCCESSFUL")) {
	                successfulBookings.add(booking);
	            }
	            else if(status.equals("CANCELED")) {
	                cancelledBookings.add(booking);
	            }
	        }
	        double totalRevenue = 0;

	        for(Map<String,Object> booking : successfulBookings){

	            totalRevenue += Double.parseDouble(
	                booking.get("price").toString()
	            );
	        }

	        model.addAttribute("totalRevenue", totalRevenue);

	        rs.close();
	        stmt.close();
	        String userSql = "SELECT * FROM users ORDER BY id DESC";

	        PreparedStatement userStmt =
	                conn.prepareStatement(userSql);

	        ResultSet userRs = userStmt.executeQuery();

	        while(userRs.next()) {

	            Map<String, Object> user = new HashMap<>();

	            user.put("id", userRs.getInt("id"));
	            user.put("fullName", userRs.getString("full_name"));
	            user.put("email", userRs.getString("email"));

	            users.add(user);
	        }

	        userRs.close();
	        userStmt.close();
	        String feedbackSql =
	                "SELECT * FROM feedback ORDER BY id DESC";

	        PreparedStatement feedbackStmt =
	                conn.prepareStatement(feedbackSql);

	        ResultSet feedbackRs =
	                feedbackStmt.executeQuery();

	        while(feedbackRs.next()) {

	            Map<String, Object> feedback =
	                    new HashMap<>();

	            feedback.put("id",
	                    feedbackRs.getInt("id"));

	            feedback.put("userName",
	                    feedbackRs.getString("user_name"));

	            feedback.put("rating",
	                    feedbackRs.getInt("rating"));

	            feedback.put("message",
	                    feedbackRs.getString("message"));

	            feedback.put("createdAt",
	                    feedbackRs.getString("created_at"));

	            feedbackList.add(feedback);
	        }

	        feedbackRs.close();
	        feedbackStmt.close();
	        conn.close();

	    } catch(Exception e) {

	        e.printStackTrace();

	    }

	    model.addAttribute("pendingBookings", pendingBookings);
	    model.addAttribute("successfulBookings", successfulBookings);
	    model.addAttribute("cancelledBookings", cancelledBookings);
	    model.addAttribute("users", users);
	    model.addAttribute("feedbackList", feedbackList);
	    return "admin-dashboard";
	}
	@GetMapping("/admin-login")
	public String adminLogin() {
	    return "admin-login";
	}

	@PostMapping("/admin-access")
	public String adminAccess(
	        @RequestParam String username,
	        @RequestParam String password,
	        HttpSession session
	) {

		if (
			    (username.equals("admin") ||
			     username.equals("Admin") ||
			     username.equals("ADMIN"))
			    &&
			    password.equals("1234")
			)
		{
	        session.setAttribute("admin", true);

	        return "redirect:/admin-dashboard";
	    }

	    return "redirect:/admin-login";
	}

	@PostMapping("/confirm-booking")
	public String confirmBooking(
	        @RequestParam int bookingId,
	        @RequestParam String paymentMethod,
	        @RequestParam String roomNumber
	) {

	    try {

	        Connection conn = DriverManager.getConnection(url, dbUser, dbPass);

	        String roomType = "";

	        String getRoom =
	                "SELECT room_type FROM booking WHERE id=?";

	        PreparedStatement getStmt =
	                conn.prepareStatement(getRoom);

	        getStmt.setInt(1, bookingId);

	        ResultSet rs = getStmt.executeQuery();

	        if(rs.next()) {
	            roomType = rs.getString("room_type");
	        }

	        String assignedRoom = roomNumber;

	        String sql =
	                "UPDATE booking " +
	                "SET status='SUCCESSFUL', " +
	                "payment_method=?, " +
	                "room_number=? " +
	                "WHERE id=?";

	        PreparedStatement stmt =
	                conn.prepareStatement(sql);

	        stmt.setString(1, paymentMethod);
	        stmt.setString(2, assignedRoom);
	        stmt.setInt(3, bookingId);

	        stmt.executeUpdate();

	        stmt.close();
	        conn.close();

	    } catch(Exception e) {

	        e.printStackTrace();

	    }

	    return "redirect:/admin-dashboard";
	}

	@PostMapping("/cancel-booking")
	public String cancelBooking(
	        @RequestParam int bookingId
	) {

	    try {

	        Connection conn = DriverManager.getConnection(url, dbUser, dbPass);

	        String sql =
	                "UPDATE booking " +
	                "SET status='CANCELED' " +
	                "WHERE id=?";

	        PreparedStatement stmt =
	                conn.prepareStatement(sql);

	        stmt.setInt(1, bookingId);

	        stmt.executeUpdate();

	        stmt.close();
	        conn.close();

	    } catch(Exception e) {

	        e.printStackTrace();

	    }

	    return "redirect:/admin-dashboard";
	}

	@PostMapping("/checkout-booking")
	public String checkoutBooking(
	        @RequestParam int bookingId
	) {

	    try {

	        Connection conn = DriverManager.getConnection(url, dbUser, dbPass);

	        String getSql =
	                "SELECT * FROM booking WHERE id=?";

	        PreparedStatement getStmt =
	                conn.prepareStatement(getSql);

	        getStmt.setInt(1, bookingId);

	        ResultSet rs = getStmt.executeQuery();

	        if(rs.next()) {

	            String insertSql =
	                    "INSERT INTO checkout_records " +
	                    "(booking_id, full_name, contact_number, email, check_in_date, check_out_date, room_type, guests, price, payment_method, room_number) " +
	                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	            PreparedStatement insertStmt =
	                    conn.prepareStatement(insertSql);

	            insertStmt.setInt(1, rs.getInt("id"));
	            insertStmt.setString(2, rs.getString("full_name"));
	            insertStmt.setString(3, rs.getString("contact_number"));
	            insertStmt.setString(4, rs.getString("email"));
	            insertStmt.setString(5, rs.getString("check_in_date"));
	            insertStmt.setString(6, rs.getString("check_out_date"));
	            insertStmt.setString(7, rs.getString("room_type"));
	            insertStmt.setInt(8, rs.getInt("guests"));
	            insertStmt.setDouble(9, rs.getDouble("price"));
	            insertStmt.setString(10, rs.getString("payment_method"));
	            insertStmt.setString(11, rs.getString("room_number"));

	            insertStmt.executeUpdate();

	            insertStmt.close();
	        }

	        String deleteSql =
	                "DELETE FROM booking WHERE id=?";

	        PreparedStatement deleteStmt =
	                conn.prepareStatement(deleteSql);

	        deleteStmt.setInt(1, bookingId);

	        deleteStmt.executeUpdate();

	        deleteStmt.close();
	        conn.close();

	    } catch(Exception e) {

	        e.printStackTrace();

	    }

	    return "redirect:/admin-dashboard";
	}
	@GetMapping("/admin-logout")
	public String adminLogout(HttpSession session) {
	    session.removeAttribute("admin");
	    return "redirect:/admin-login";
	}
}