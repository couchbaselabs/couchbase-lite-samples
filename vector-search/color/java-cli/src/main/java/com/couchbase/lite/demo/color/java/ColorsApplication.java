package com.couchbase.lite.demo.color.java;

import androidx.annotation.NonNull;

import java.io.IOException;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.demo.color.java.service.DBService;


public class ColorsApplication {

    public static void main(@NonNull final String[] args) { new ColorsApplication().run(); }

    public void run() {
        DBService svc = null;
        try {
            svc = new DBService().init();
            new UI(svc).run();
        }
        catch (IOException | CouchbaseLiteException e) {
            throw new RuntimeException("Application failure", e);
        }
        finally {
            if (svc != null) {
                try { svc.close(); }
                catch (CouchbaseLiteException ignore) { }
            }
        }
    }
}
