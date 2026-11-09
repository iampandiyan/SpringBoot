package com.example.trade.service;

import com.example.trade.config.TradeProperties;
import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.repo.TradeRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// @Transactional on the CLASS: every public method runs in a transaction, opened by the Spring proxy
// before the method and committed (or rolled back) after it. Reads override this with readOnly = true.
@Service
@Transactional
public class TradeService {

    private final TradeRepository repository;
    private final TradeProperties props;

    // Constructor injection: single constructor, so no @Autowired needed (since Spring 4.3).
    // The fields are final, so the service can never exist half-wired, and unit tests can call new TradeService(fake, props).
    public TradeService(TradeRepository repository, TradeProperties props) {
        this.repository = repository;
        this.props = props;
    }

    public Trade create(String isin, BigDecimal quantity, BigDecimal price) {
        // The limit comes from configuration (trade.max-quantity), so each environment can set its own.
        if (quantity.compareTo(props.maxQuantity()) > 0) {
            throw new BusinessRuleException("Quantity " + quantity + " exceeds limit " + props.maxQuantity());
        }
        return repository.save(new Trade(null, isin, quantity, price, TradeStatus.NEW));
    }

    @Transactional(readOnly = true)     // hint: no dirty checking, and some databases can route it to a replica
    public Trade get(Long id) {
        return repository.findById(id).orElseThrow(() -> new TradeNotFoundException(id));
    }

    // status == null means "no filter".
    @Transactional(readOnly = true)
    public List<Trade> list(TradeStatus status) {
        return repository.findAll().stream()
                .filter(t -> status == null || t.status() == status)
                .toList();
    }

    // DEMO of the self-invocation trap: the inner this.settle(id) call goes straight to the real
    // object, NOT through the Spring proxy, so TimingAspect never sees it (and neither would @Transactional).
    public Trade settleViaSelfCall(Long id) {
        return this.settle(id);
    }

    public Trade settle(Long id) {
        Trade t = get(id);
        if (t.status() == TradeStatus.SETTLED) {
            throw new TradeStateException("Trade " + id + " is already settled");
        }
        return repository.save(t.withStatus(TradeStatus.SETTLED));
    }

    // ALL OR NOTHING: if any id is unknown, TradeNotFoundException (unchecked) rolls back the whole transaction,
    // including the trades already settled earlier in the loop.
    public void settleAll(List<Long> ids) {
        for (Long id : ids) {
            settle(id);
        }
    }

    // DEMO 1: a CHECKED exception does NOT roll back by default. The settle below is committed.
    public void settleThenFailChecked(Long id) throws Exception {
        settle(id);
        throw new Exception("checked failure after settle");
    }

    // DEMO 2: rollbackFor = Exception.class makes the same failure roll back.
    @Transactional(rollbackFor = Exception.class)
    public void settleThenFailCheckedWithRollbackFor(Long id) throws Exception {
        settle(id);
        throw new Exception("checked failure after settle");
    }
}
