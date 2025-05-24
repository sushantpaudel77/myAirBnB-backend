package com.projects.airbnb.service;


import com.projects.airbnb.entity.Booking;

public interface CheckOutService {

    String getCheckOutSession(Booking booking, String successUrl, String failureUrl);
}
