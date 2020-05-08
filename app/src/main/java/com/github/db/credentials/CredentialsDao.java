package com.github.db.credentials;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface CredentialsDao {

    @Query("SELECT * FROM Credential WHERE id = 1")
    Credential getCredentials();

    @Query("UPDATE Credential SET password = :password WHERE id = 1")
    void modifyPassword(String password);

    @Query("UPDATE Credential SET username = :username WHERE id = 1")
    void modifyUser(String username);

    @Query("UPDATE Credential SET ipAddress = :ipAddress WHERE id = 1")
    void changeIpAddress(String ipAddress);

    @Insert
    void insert(Credential credential);
}
