package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a render operation. The failure probability is configurable so the worker's
 * retry path can be exercised without making tests flaky.
 */
@Service
public class RenderService {

    private final long renderDelayMs;
    private final double transientFailureProbability;

    public RenderService(
            @Value("${render.delay-ms:250}") long renderDelayMs,
            @Value("${render.transient-failure-probability:0.20}") double transientFailureProbability) {
        if (renderDelayMs < 0) {
            throw new IllegalArgumentException("render.delay-ms must be >= 0");
        }
        if (transientFailureProbability < 0 || transientFailureProbability > 1) {
            throw new IllegalArgumentException(
                    "render.transient-failure-probability must be between 0 and 1");
        }
        this.renderDelayMs = renderDelayMs;
        this.transientFailureProbability = transientFailureProbability;
    }

    public String render(Job job) throws InterruptedException, TransientRenderException {
        if (renderDelayMs > 0) {
            Thread.sleep(renderDelayMs);
        }

        if (ThreadLocalRandom.current().nextDouble() < transientFailureProbability) {
            throw new TransientRenderException("Transient render failure");
        }

        return "Rendered job " + job.getId();
    }

    public static class TransientRenderException extends Exception {
        public TransientRenderException(String message) {
            super(message);
        }
    }
}
