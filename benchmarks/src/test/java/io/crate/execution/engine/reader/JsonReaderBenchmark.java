/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.execution.engine.reader;

import io.crate.analyze.CopyFromParserProperties;
import io.crate.metadata.NodeContext;
import io.crate.metadata.SearchPath;
import io.crate.metadata.settings.SessionSettings;
import io.crate.data.BatchIterator;
import io.crate.data.Input;
import io.crate.data.Row;
import io.crate.execution.engine.collect.files.FileReadingIterator;
import io.crate.execution.engine.collect.files.LineCollectorExpression;
import io.crate.execution.engine.collect.files.LocalFsFileInputFactory;
import io.crate.expression.InputFactory;
import io.crate.expression.reference.file.FileLineReferenceResolver;
import io.crate.metadata.Functions;
import io.crate.metadata.Reference;
import io.crate.metadata.TransactionContext;
import io.crate.types.DataTypes;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.crate.execution.dsl.phases.FileUriCollectPhase.InputFormat.JSON;
import static io.crate.testing.TestingHelpers.createReference;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class JsonReaderBenchmark {

    private String fileUri;
    private InputFactory inputFactory;
    private TransactionContext txnCtx = TransactionContext.of(
        new SessionSettings("dummyUser",
                            SearchPath.createSearchPathFrom("dummySchema")));
    File tempFile;

    @Setup
    public void create_temp_file_and_uri() throws IOException {
        NodeContext nodeCtx = new NodeContext(new Functions(Map.of()));
        inputFactory = new InputFactory(nodeCtx);
        tempFile = File.createTempFile("temp", null);
        fileUri = tempFile.toURI().getPath();
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
            writer.write("{\"name\": \"Arthur\", \"id\": 4\\n");
            writer.write("{\"id\": 5, \"name\": \"Trillian\"\n");
            writer.write("{\"id\": 5, \"name\": \"Emma\"\n");
            writer.write("{\"id\": 9, \"name\": \"Emily\"\n");
            writer.write("{\"id\": 5, \"name\": \"Sarah\"\n");
            writer.write("{\"id\": 5, \"name\": \"John\"\n");
            writer.write("{\"id\": 9, \"name\": \"Mical\"\n");
            writer.write("{\"id\": 5, \"name\": \"Mary\"\n");
            writer.write("{\"id\": 9, \"name\": \"Jimmy\"\n");
            writer.write("{\"id\": 5, \"name\": \"Tom\"\n");
            writer.write("{\"id\": 0, \"name\": \"Neil\"\n");
            writer.write("{\"id\": 5, \"name\": \"Rose\"\n");
            writer.write("{\"id\": 5, \"name\": \"Gobnait\"\n");
            writer.write("{\"id\": 1, \"name\": \"Rory\"\n");
            writer.write("{\"id\": 11, \"name\": \"Martin\"\n");
            writer.write("{\"id\": 5, \"name\": \"Trillian\"\n");
            writer.write("{\"id\": 5, \"name\": \"Emma\"\n");
            writer.write("{\"id\": 9, \"name\": \"Emily\"\n");
            writer.write("{\"id\": 5, \"name\": \"Sarah\"\n");
            writer.write("{\"id\": 5, \"name\": \"John\"\n");
            writer.write("{\"id\": 9, \"name\": \"Mical\"\n");
            writer.write("{\"id\": 5, \"name\": \"Mary\"\n");
            writer.write("{\"id\": 9, \"name\": \"Jimmy\"\n");
            writer.write("{\"id\": 5, \"name\": \"Tom\"\n");
            writer.write("{\"id\": 0, \"name\": \"Neil\"\n");
            writer.write("{\"id\": 5, \"name\": \"Rose\"\n");
            writer.write("{\"id\": 5, \"name\": \"Gobnait\"\n");
            writer.write("{\"id\": 1, \"name\": \"Rory\"\n");
            writer.write("{\"id\": 11, \"name\": \"Martin\"\n");
        }
    }

    @Benchmark()
    public void measureFileReadingIteratorForJson(Blackhole blackhole) {
        Reference raw = createReference("_raw", DataTypes.STRING);
        InputFactory.Context<LineCollectorExpression<?>> ctx = inputFactory.ctxForRefs(
            txnCtx, FileLineReferenceResolver::getImplementation);

        List<Input<?>> inputs = Collections.singletonList(ctx.add(raw));
        BatchIterator<Row> batchIterator = FileReadingIterator.newInstance(
            Collections.singletonList(fileUri),
            inputs,
            ctx.expressions(),
            null,
            Map.of(
                LocalFsFileInputFactory.NAME, new LocalFsFileInputFactory()),
            false,
            1,
            0,
            CopyFromParserProperties.DEFAULT,
            JSON);

        while (batchIterator.moveNext()) {
            blackhole.consume(batchIterator.currentElement().get(0));
        }
    }

    @TearDown
    public void cleanup() throws InterruptedException {
        tempFile.deleteOnExit();
    }
}
