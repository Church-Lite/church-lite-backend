package com.smartverse.churchlitebackend.service.translation;

import com.smartverse.churchlitebackend_gen.enums.SubscriptionFeature;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionService;
import com.smartverse.churchlitebackend_gen.common.RequestData;
import com.smartverse.churchlitebackend_gen.common.ResponseData;
import com.smartverse.churchlitebackend_gen.dtos.TranslationDTO;
import com.smartverse.churchlitebackend_gen.services.TranslationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TranslationBusinessService extends TranslationService {
    private final SubscriptionService subscriptionService;

    public TranslationBusinessService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    @Transactional
    public TranslationDTO save(TranslationDTO dto) {
        requireFeature();
        return super.save(dto);
    }

    @Override
    @Transactional
    public TranslationDTO update(TranslationDTO dto, UUID id) {
        requireFeature();
        return super.update(dto, id);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        requireFeature();
        super.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TranslationDTO get(UUID id) {
        requireFeature();
        return super.get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseData getAll(RequestData input) {
        requireFeature();
        return super.getAll(input);
    }

    private void requireFeature() {
        subscriptionService.requireFeature(SubscriptionFeature.CUSTOM_TRANSLATIONS);
    }
}
