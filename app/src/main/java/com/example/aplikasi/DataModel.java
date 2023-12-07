package com.example.aplikasi;

public class DataModel {
    private String name;
    private String phoneNumber;
    private String email; // Menambahkan atribut untuk email pengguna
    private String documentId;

    public DataModel(String name, String phoneNumber, String email, String documentId) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.documentId = documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
