package com.ms.campuslab_bff.controller;

import com.ms.campuslab_bff.service.AccountDirectory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountDirectoryController {
    private final AccountDirectory directory;

    public AccountDirectoryController(AccountDirectory directory) {
        this.directory = directory;
    }

    @GetMapping({"/api/bff/admin/directorio", "/api/bff/academic/admin/directorio"})
    public ResponseEntity<AccountDirectory.Result> list() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(directory.list());
    }
}
