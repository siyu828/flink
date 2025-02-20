/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.planner.plan.nodes.exec.stream;

import org.apache.flink.FlinkVersion;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.planner.delegation.PlannerBase;
import org.apache.flink.table.planner.plan.nodes.exec.ExecNodeContext;
import org.apache.flink.table.planner.plan.nodes.exec.ExecNodeMetadata;
import org.apache.flink.table.planner.plan.nodes.exec.InputProperty;
import org.apache.flink.table.planner.plan.nodes.exec.spec.PartitionSpec;
import org.apache.flink.table.planner.plan.nodes.exec.spec.SortSpec;
import org.apache.flink.table.planner.plan.utils.RankProcessStrategy;
import org.apache.flink.table.runtime.operators.rank.ConstantRankRange;
import org.apache.flink.table.runtime.operators.rank.RankType;
import org.apache.flink.table.types.logical.RowType;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/** {@link StreamExecNode} for Sort with limit. */
@ExecNodeMetadata(
        name = "stream-exec-sort-limit",
        version = 1,
        minPlanVersion = FlinkVersion.v1_15,
        minStateVersion = FlinkVersion.v1_15)
public class StreamExecSortLimit extends StreamExecRank {

    private final long limitEnd;

    public StreamExecSortLimit(
            SortSpec sortSpec,
            long limitStart,
            long limitEnd,
            RankProcessStrategy rankStrategy,
            boolean generateUpdateBefore,
            InputProperty inputProperty,
            RowType outputType,
            String description) {
        this(
                ExecNodeContext.newNodeId(),
                ExecNodeContext.newContext(StreamExecSortLimit.class),
                sortSpec,
                new ConstantRankRange(limitStart + 1, limitEnd),
                rankStrategy,
                generateUpdateBefore,
                Collections.singletonList(inputProperty),
                outputType,
                description);
    }

    @JsonCreator
    public StreamExecSortLimit(
            @JsonProperty(FIELD_NAME_ID) int id,
            @JsonProperty(FIELD_NAME_TYPE) ExecNodeContext context,
            @JsonProperty(FIELD_NAME_SORT_SPEC) SortSpec sortSpec,
            @JsonProperty(FIELD_NAME_RANK_RANG) ConstantRankRange rankRange,
            @JsonProperty(FIELD_NAME_RANK_STRATEGY) RankProcessStrategy rankStrategy,
            @JsonProperty(FIELD_NAME_GENERATE_UPDATE_BEFORE) boolean generateUpdateBefore,
            @JsonProperty(FIELD_NAME_INPUT_PROPERTIES) List<InputProperty> inputProperties,
            @JsonProperty(FIELD_NAME_OUTPUT_TYPE) RowType outputType,
            @JsonProperty(FIELD_NAME_DESCRIPTION) String description) {

        super(
                id,
                context,
                RankType.ROW_NUMBER,
                PartitionSpec.ALL_IN_ONE,
                sortSpec,
                rankRange,
                rankStrategy,
                false, // outputRankNumber
                generateUpdateBefore,
                inputProperties,
                outputType,
                description);
        this.limitEnd = rankRange.getRankEnd();
    }

    @Override
    protected Transformation<RowData> translateToPlanInternal(PlannerBase planner) {
        if (limitEnd == Long.MAX_VALUE) {
            throw new TableException(
                    "FETCH is missed, which on streaming table is not supported currently.");
        }
        return super.translateToPlanInternal(planner);
    }
}
