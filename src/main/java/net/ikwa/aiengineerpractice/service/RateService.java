// src/main/java/net/ikwa/aiengineerpractice/service/RateService.java
package net.ikwa.aiengineerpractice.service;

import net.ikwa.aiengineerpractice.model.RateModel;
import net.ikwa.aiengineerpractice.repo.RateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RateService {

    private final RateRepository rateRepository;

    public RateService(RateRepository rateRepository) {
        this.rateRepository = rateRepository;
    }

    public RateModel getCurrentRates() {
        RateModel model = rateRepository.findTopByOrderByUpdatedAtDesc();
        if (model == null) {
            throw new IllegalStateException("No rates configured yet");
        }
        return model;
    }

    // used by admin dashboard to save/update
    public RateModel saveRates(RateModel model) {
        // always stamp a fresh updatedAt so /current and history work correctly
        model.setUpdatedAt(LocalDateTime.now());
        return rateRepository.save(model);
    }

    // optional: full history list (for dashboard rate history table)
    public List<RateModel> getRateHistory() {
        return rateRepository.findAllByOrderByUpdatedAtDesc();
    }

    // helper to build a nice plain-text response for chat
    public String buildRateMessage() {
        RateModel r = getCurrentRates();

        // format numbers nicely with no decimals
        String derivBuy   = formatNaira(r.getDerivDeposit());
        String derivSell  = formatNaira(r.getDerivWithdraw());
        String cryptoBuy  = formatNaira(r.getCryptoDeposit());
        String cryptoSell = formatNaira(r.getCryptoWithdraw());
        String cashRate   = formatNaira(r.getCashDollar());

        // Clean, spaced-out blocks (no ASCII table)
        return """
                Here are our current live quotes (₦ per $1, subject to change):

                Deriv USD:
                Deposit (Buy): ₦%s per $1
                Withdraw (Sell): ₦%s per $1

                Crypto (USDT, BTC, ETH…):
                Deposit (Buy): ₦%s per $1
                Withdraw (Sell): ₦%s per $1

                Cash dollar (physical USD):
                Spot rate: ₦%s per $1
                
                Note: Below 5$ attracts N100 service fees.

                
                Please note that rates may adjust slightly based on liquidity and market movement.
                """.formatted(
                derivBuy,
                derivSell,
                cryptoBuy,
                cryptoSell,
                cashRate
        );
    }

    private String formatNaira(Double value) {
        if (value == null) return "-";
        // no decimals, with thousand separators
        return String.format("%,.0f", value);
    }

    // small helper to keep columns visually aligned in monospace-like layout
    // (currently not used in the string above, but kept for future flexibility)
    private String padLeft(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width - s.length(); i++) {
            sb.append(' ');
        }
        sb.append(s);
        return sb.toString();
    }
}
