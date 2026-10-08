package com.zovira.common.sequence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Generates monotonically increasing Long identifiers for MongoDB collections,
 * ensuring seamless compatibility with numeric ID schemas and API responses.
 */
@Service
public class SequenceGeneratorService {

    private final MongoOperations mongoOperations;

    public SequenceGeneratorService(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public long generateSequence(String seqName) {
        DatabaseSequence counter = mongoOperations.findAndModify(
                query(where("_id").is(seqName)),
                new Update().inc("seq", 1),
                options().returnNew(true).upsert(true),
                DatabaseSequence.class);
        return counter != null ? counter.getSeq() : 1;
    }

    @Document(collection = "database_sequences")
    public static class DatabaseSequence {
        @Id
        private String id;
        private long seq;

        public DatabaseSequence() {}

        public DatabaseSequence(String id, long seq) {
            this.id = id;
            this.seq = seq;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public long getSeq() { return seq; }
        public void setSeq(long seq) { this.seq = seq; }
    }
}
