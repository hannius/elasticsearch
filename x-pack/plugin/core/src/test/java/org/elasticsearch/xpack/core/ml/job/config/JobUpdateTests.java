/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.job.config;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.AbstractSerializingTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

public class JobUpdateTests extends AbstractSerializingTestCase<JobUpdate> {

    private boolean useInternalParser = randomBoolean();

    @Override
    protected JobUpdate createTestInstance() {
        JobUpdate.Builder update = new JobUpdate.Builder(randomAlphaOfLength(4));
        if (randomBoolean()) {
            int groupsNum = randomIntBetween(0, 10);
            List<String> groups = new ArrayList<>(groupsNum);
            for (int i = 0; i < groupsNum; i++) {
                groups.add(JobTests.randomValidJobId());
            }
            update.setGroups(groups);
        }
        if (randomBoolean()) {
            update.setDescription(randomAlphaOfLength(20));
        }
        if (randomBoolean()) {
            int size = randomInt(10);
            List<JobUpdate.DetectorUpdate> detectorUpdates = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                String detectorDescription = null;
                if (randomBoolean()) {
                    detectorDescription = randomAlphaOfLength(12);
                }
                List<DetectionRule> detectionRules = null;
                if (randomBoolean()) {
                    detectionRules = new ArrayList<>();
                    Condition condition = new Condition(Operator.GT, "5");
                    detectionRules.add(new DetectionRule.Builder(
                            Collections.singletonList(new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, null, null, condition, null)))
                            .setTargetFieldName("foo").build());
                }
                detectorUpdates.add(new JobUpdate.DetectorUpdate(i, detectorDescription, detectionRules));
            }
            update.setDetectorUpdates(detectorUpdates);
        }
        if (randomBoolean()) {
            update.setModelPlotConfig(new ModelPlotConfig(randomBoolean(), randomAlphaOfLength(10)));
        }
        if (randomBoolean()) {
            update.setAnalysisLimits(AnalysisLimitsTests.createRandomized());
        }
        if (randomBoolean()) {
            update.setRenormalizationWindowDays(randomNonNegativeLong());
        }
        if (randomBoolean()) {
            update.setBackgroundPersistInterval(TimeValue.timeValueHours(randomIntBetween(1, 24)));
        }
        if (randomBoolean()) {
            update.setModelSnapshotRetentionDays(randomNonNegativeLong());
        }
        if (randomBoolean()) {
            update.setResultsRetentionDays(randomNonNegativeLong());
        }
        if (randomBoolean()) {
            update.setCategorizationFilters(Arrays.asList(generateRandomStringArray(10, 10, false)));
        }
        if (randomBoolean()) {
            update.setCustomSettings(Collections.singletonMap(randomAlphaOfLength(10), randomAlphaOfLength(10)));
        }
        if (useInternalParser && randomBoolean()) {
            update.setModelSnapshotId(randomAlphaOfLength(10));
        }
        if (useInternalParser && randomBoolean()) {
            update.setModelSnapshotMinVersion(Version.CURRENT);
        }
        if (useInternalParser && randomBoolean()) {
            update.setEstablishedModelMemory(randomNonNegativeLong());
        }
        if (useInternalParser && randomBoolean()) {
            update.setJobVersion(randomFrom(Version.CURRENT, Version.V_6_2_0, Version.V_6_1_0));
        }

        return update.build();
    }

    @Override
    protected Writeable.Reader<JobUpdate> instanceReader() {
        return JobUpdate::new;
    }

    @Override
    protected JobUpdate doParseInstance(XContentParser parser) {
        if (useInternalParser) {
            return JobUpdate.INTERNAL_PARSER.apply(parser, null).build();
        } else {
            return JobUpdate.EXTERNAL_PARSER.apply(parser, null).build();
        }
    }

    public void testMergeWithJob() {
        List<JobUpdate.DetectorUpdate> detectorUpdates = new ArrayList<>();
        List<DetectionRule> detectionRules1 = Collections.singletonList(new DetectionRule.Builder(
                Collections.singletonList(new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, null, null, new Condition(Operator.GT, "5")
                        , null)))
                .setTargetFieldName("mlcategory").build());
        detectorUpdates.add(new JobUpdate.DetectorUpdate(0, "description-1", detectionRules1));
        List<DetectionRule> detectionRules2 = Collections.singletonList(new DetectionRule.Builder(Collections.singletonList(
                new RuleCondition(RuleConditionType.NUMERICAL_ACTUAL, null, null, new Condition(Operator.GT, "5"), null)))
                .setTargetFieldName("host").build());
        detectorUpdates.add(new JobUpdate.DetectorUpdate(1, "description-2", detectionRules2));

        ModelPlotConfig modelPlotConfig = new ModelPlotConfig(randomBoolean(), randomAlphaOfLength(10));
        AnalysisLimits analysisLimits = new AnalysisLimits(randomNonNegativeLong(), randomNonNegativeLong());
        List<String> categorizationFilters = Arrays.asList(generateRandomStringArray(10, 10, false));
        Map<String, Object> customSettings = Collections.singletonMap(randomAlphaOfLength(10), randomAlphaOfLength(10));

        JobUpdate.Builder updateBuilder = new JobUpdate.Builder("foo");
        updateBuilder.setGroups(Arrays.asList("group-1", "group-2"));
        updateBuilder.setDescription("updated_description");
        updateBuilder.setDetectorUpdates(detectorUpdates);
        updateBuilder.setModelPlotConfig(modelPlotConfig);
        updateBuilder.setAnalysisLimits(analysisLimits);
        updateBuilder.setBackgroundPersistInterval(TimeValue.timeValueHours(randomIntBetween(1, 24)));
        updateBuilder.setResultsRetentionDays(randomNonNegativeLong());
        updateBuilder.setModelSnapshotRetentionDays(randomNonNegativeLong());
        updateBuilder.setRenormalizationWindowDays(randomNonNegativeLong());
        updateBuilder.setCategorizationFilters(categorizationFilters);
        updateBuilder.setCustomSettings(customSettings);
        updateBuilder.setModelSnapshotId(randomAlphaOfLength(10));
        updateBuilder.setJobVersion(Version.V_6_1_0);
        JobUpdate update = updateBuilder.build();

        Job.Builder jobBuilder = new Job.Builder("foo");
        jobBuilder.setGroups(Arrays.asList("group-1"));
        Detector.Builder d1 = new Detector.Builder("info_content", "domain");
        d1.setOverFieldName("mlcategory");
        Detector.Builder d2 = new Detector.Builder("min", "field");
        d2.setOverFieldName("host");
        AnalysisConfig.Builder ac = new AnalysisConfig.Builder(Arrays.asList(d1.build(), d2.build()));
        ac.setCategorizationFieldName("cat_field");
        jobBuilder.setAnalysisConfig(ac);
        jobBuilder.setDataDescription(new DataDescription.Builder());
        jobBuilder.setCreateTime(new Date());

        Job updatedJob = update.mergeWithJob(jobBuilder.build(), new ByteSizeValue(0L));

        assertEquals(update.getGroups(), updatedJob.getGroups());
        assertEquals(update.getDescription(), updatedJob.getDescription());
        assertEquals(update.getModelPlotConfig(), updatedJob.getModelPlotConfig());
        assertEquals(update.getAnalysisLimits(), updatedJob.getAnalysisLimits());
        assertEquals(update.getRenormalizationWindowDays(), updatedJob.getRenormalizationWindowDays());
        assertEquals(update.getBackgroundPersistInterval(), updatedJob.getBackgroundPersistInterval());
        assertEquals(update.getModelSnapshotRetentionDays(), updatedJob.getModelSnapshotRetentionDays());
        assertEquals(update.getResultsRetentionDays(), updatedJob.getResultsRetentionDays());
        assertEquals(update.getCategorizationFilters(), updatedJob.getAnalysisConfig().getCategorizationFilters());
        assertEquals(update.getCustomSettings(), updatedJob.getCustomSettings());
        assertEquals(update.getModelSnapshotId(), updatedJob.getModelSnapshotId());
        assertEquals(update.getJobVersion(), updatedJob.getJobVersion());
        for (JobUpdate.DetectorUpdate detectorUpdate : update.getDetectorUpdates()) {
            assertNotNull(updatedJob.getAnalysisConfig().getDetectors().get(detectorUpdate.getDetectorIndex()).getDetectorDescription());
            assertEquals(detectorUpdate.getDescription(),
                    updatedJob.getAnalysisConfig().getDetectors().get(detectorUpdate.getDetectorIndex()).getDetectorDescription());
            assertNotNull(updatedJob.getAnalysisConfig().getDetectors().get(detectorUpdate.getDetectorIndex()).getDetectorDescription());
            assertEquals(detectorUpdate.getRules(),
                    updatedJob.getAnalysisConfig().getDetectors().get(detectorUpdate.getDetectorIndex()).getRules());
        }
    }

    public void testIsAutodetectProcessUpdate() {
        JobUpdate update = new JobUpdate.Builder("foo").build();
        assertFalse(update.isAutodetectProcessUpdate());
        update = new JobUpdate.Builder("foo").setModelPlotConfig(new ModelPlotConfig(true, "ff")).build();
        assertTrue(update.isAutodetectProcessUpdate());
        update = new JobUpdate.Builder("foo").setDetectorUpdates(Collections.singletonList(mock(JobUpdate.DetectorUpdate.class))).build();
        assertTrue(update.isAutodetectProcessUpdate());
    }

    public void testUpdateAnalysisLimitWithValueGreaterThanMax() {
        Job.Builder jobBuilder = new Job.Builder("foo");
        Detector.Builder d1 = new Detector.Builder("info_content", "domain");
        d1.setOverFieldName("mlcategory");
        Detector.Builder d2 = new Detector.Builder("min", "field");
        d2.setOverFieldName("host");
        AnalysisConfig.Builder ac = new AnalysisConfig.Builder(Arrays.asList(d1.build(), d2.build()));
        ac.setCategorizationFieldName("cat_field");
        jobBuilder.setAnalysisConfig(ac);
        jobBuilder.setDataDescription(new DataDescription.Builder());
        jobBuilder.setCreateTime(new Date());
        jobBuilder.setAnalysisLimits(new AnalysisLimits(256L, null));

        JobUpdate update = new JobUpdate.Builder("foo").setAnalysisLimits(new AnalysisLimits(1024L, null)).build();

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class,
                () -> update.mergeWithJob(jobBuilder.build(), new ByteSizeValue(512L, ByteSizeUnit.MB)));
        assertEquals("model_memory_limit [1gb] must be less than the value of the xpack.ml.max_model_memory_limit setting [512mb]",
                e.getMessage());
    }

    public void testUpdate_withAnalysisLimitsPreviouslyUndefined() {
        Job.Builder jobBuilder = new Job.Builder("foo");
        Detector.Builder d1 = new Detector.Builder("info_content", "domain");
        AnalysisConfig.Builder ac = new AnalysisConfig.Builder(Collections.singletonList(d1.build()));
        jobBuilder.setAnalysisConfig(ac);
        jobBuilder.setDataDescription(new DataDescription.Builder());
        jobBuilder.setCreateTime(new Date());
        jobBuilder.validateAnalysisLimitsAndSetDefaults(null);

        JobUpdate update = new JobUpdate.Builder("foo").setAnalysisLimits(new AnalysisLimits(2048L, 5L)).build();
        Job updated = update.mergeWithJob(jobBuilder.build(), new ByteSizeValue(0L));
        assertThat(updated.getAnalysisLimits().getModelMemoryLimit(), equalTo(2048L));
        assertThat(updated.getAnalysisLimits().getCategorizationExamplesLimit(), equalTo(5L));

        JobUpdate updateAboveMaxLimit = new JobUpdate.Builder("foo").setAnalysisLimits(new AnalysisLimits(8000L, null)).build();

        Exception e = expectThrows(ElasticsearchStatusException.class,
                () -> updateAboveMaxLimit.mergeWithJob(jobBuilder.build(), new ByteSizeValue(5000L, ByteSizeUnit.MB)));
        assertEquals("model_memory_limit [7.8gb] must be less than the value of the xpack.ml.max_model_memory_limit setting [4.8gb]",
                e.getMessage());

        updateAboveMaxLimit.mergeWithJob(jobBuilder.build(), new ByteSizeValue(10000L, ByteSizeUnit.MB));
    }
}
