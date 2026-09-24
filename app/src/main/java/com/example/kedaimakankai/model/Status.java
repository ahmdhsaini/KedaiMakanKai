package com.example.kedaimakankai.model;

import com.google.gson.annotations.SerializedName;

public class Status {

    @SerializedName("StatusID")
    public Integer StatusID;

    @SerializedName("BookingStatus")
    public String BookingStatus; // PENDING / APPROVED / REJECTED / CANCELLED

    @SerializedName("TableNumber")
    public Integer TableNumber;

    // API returns AdminID as INT or OBJECT; we keep as Integer for now
    @SerializedName("AdminID")
    public Integer AdminID;

    // API may return BookID as INT, String, or nested object
    @SerializedName(value = "BookID", alternate = {"BookId", "book_id", "Book"})
    public Object BookID;

    @SerializedName("StatusTime")
    public String StatusTime;

    @SerializedName("AdminNote")
    public String AdminNote;

    /**
     * Resolve BookID regardless of whether API sends int, string, or nested object.
     */
    public Integer getBookId() {
        if (BookID == null) return null;
        try {
            if (BookID instanceof Number) {
                return ((Number) BookID).intValue();
            }
            if (BookID instanceof String) {
                return Integer.parseInt(((String) BookID).trim());
            }
            if (BookID instanceof java.util.Map) {
                Object inner = ((java.util.Map<?, ?>) BookID).get("BookID");
                if (inner instanceof Number) return ((Number) inner).intValue();
                if (inner instanceof String) return Integer.parseInt(((String) inner).trim());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
