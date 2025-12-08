// src/main/java/net/ikwa/aiengineerpractice/model/RateModel.java
package net.ikwa.aiengineerpractice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rates")
@AllArgsConstructor
@NoArgsConstructor
public class RateModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Example fields – customize as you like
    private Double derivDeposit;   // e.g. 1470
    private Double derivWithdraw;  // e.g. 1430
    private Double cryptoDeposit;  // e.g. 1490
    private Double cryptoWithdraw; // e.g. 1450
    private Double cashDollar;     // e.g. 1430

    private LocalDateTime updatedAt = LocalDateTime.now();

    // getters & setters...

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getDerivDeposit() {
        return derivDeposit;
    }

    public void setDerivDeposit(Double derivDeposit) {
        this.derivDeposit = derivDeposit;
    }

    public Double getDerivWithdraw() {
        return derivWithdraw;
    }

    public void setDerivWithdraw(Double derivWithdraw) {
        this.derivWithdraw = derivWithdraw;
    }

    public Double getCryptoDeposit() {
        return cryptoDeposit;
    }

    public void setCryptoDeposit(Double cryptoDeposit) {
        this.cryptoDeposit = cryptoDeposit;
    }

    public Double getCryptoWithdraw() {
        return cryptoWithdraw;
    }

    public void setCryptoWithdraw(Double cryptoWithdraw) {
        this.cryptoWithdraw = cryptoWithdraw;
    }

    public Double getCashDollar() {
        return cashDollar;
    }

    public void setCashDollar(Double cashDollar) {
        this.cashDollar = cashDollar;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
