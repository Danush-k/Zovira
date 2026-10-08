package com.zovira.common.sequence;

import com.zovira.common.entity.BaseEntity;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

/**
 * Automatically assigns a sequential Long id to any BaseEntity prior to insertion in MongoDB.
 */
@Component
public class BaseEntityModelListener extends AbstractMongoEventListener<BaseEntity> {

    private final SequenceGeneratorService sequenceGenerator;

    public BaseEntityModelListener(SequenceGeneratorService sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<BaseEntity> event) {
        BaseEntity source = event.getSource();
        if (source != null && (source.getId() == null || source.getId() <= 0)) {
            String collectionName = event.getCollectionName();
            if (collectionName == null || collectionName.isBlank()) {
                collectionName = source.getClass().getSimpleName().toLowerCase();
            }
            source.setId(sequenceGenerator.generateSequence(collectionName + "_seq"));
        }
    }
}
