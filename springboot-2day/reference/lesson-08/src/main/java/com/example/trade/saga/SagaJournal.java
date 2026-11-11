package com.example.trade.saga;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

// A running log of what the saga did, in order: the audit trail an operator reads to understand a settlement,
// and the thing our tests read to prove that compensation ran in REVERSE order.
@Component
public class SagaJournal {

    private final List<String> entries = new CopyOnWriteArrayList<>();

    public void add(String entry) { entries.add(entry); }

    public List<String> entries() { return List.copyOf(entries); }

    public void clear() { entries.clear(); }
}
