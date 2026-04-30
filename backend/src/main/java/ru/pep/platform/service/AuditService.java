package ru.pep.platform.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.AuditEvent;
import ru.pep.platform.repository.AuditEventRepository;

@Service
public class AuditService {

    private final AuditEventRepository auditEvents;

    public AuditService(AuditEventRepository auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Transactional
    public void record(AppUser actor, String action, String targetType, UUID targetId, String metadataJson) {
        auditEvents.save(new AuditEvent(actor, action, targetType, targetId, metadataJson));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> latest() {
        return auditEvents.findTop100ByOrderByCreatedAtDesc();
    }
}
