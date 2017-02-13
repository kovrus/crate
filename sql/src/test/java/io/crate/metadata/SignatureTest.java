/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.metadata;

import com.google.common.collect.ImmutableList;
import io.crate.test.integration.CrateUnitTest;
import io.crate.types.DataType;
import io.crate.types.DataTypes;
import io.crate.types.SetType;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;

public class SignatureTest extends CrateUnitTest {

    private void assertWrongSignature(Signature.SignatureOperator op, List<DataType> in) {
        assertThat(op.apply(in), nullValue());
    }

    private void assertSignature(Signature.SignatureOperator op, List<DataType> in, DataType... out) {
        assertThat(op.apply(in), contains(out));
    }

    private void assertSameSignature(Signature.SignatureOperator op, List<DataType> in) {
        assertThat(op.apply(in), contains(in.toArray()));
    }

    @Test
    public void testSimpleMatch() {
        Signature.SignatureOperator signature = Signature.of(DataTypes.STRING);
        assertSameSignature(signature, ImmutableList.of(DataTypes.STRING));
        assertSignature(signature, ImmutableList.of(DataTypes.UNDEFINED), DataTypes.STRING);
        assertWrongSignature(signature, ImmutableList.of(DataTypes.LONG));
    }

    @Test
    public void testMatchSize() {
        Signature.SignatureOperator signature = Signature.of(1);
        // the size matcher cannot rewrite so null stays null
        assertSameSignature(signature, ImmutableList.of(DataTypes.UNDEFINED));
        assertSameSignature(signature, ImmutableList.of(DataTypes.BOOLEAN));
        assertWrongSignature(signature, ImmutableList.of(DataTypes.BOOLEAN, DataTypes.BOOLEAN));
    }

    @Test
    public void testMatchAnyArray() {
        Signature.SignatureOperator sig = Signature.of(Signature.ArgMatcher.ANY_ARRAY);
        assertSameSignature(sig, ImmutableList.of(DataTypes.DOUBLE_ARRAY));
        assertSameSignature(sig, ImmutableList.of(DataTypes.OBJECT_ARRAY));
        assertWrongSignature(sig, ImmutableList.of(DataTypes.STRING));
        assertSameSignature(sig, ImmutableList.of(DataTypes.UNDEFINED));
    }

    @Test
    public void testMatchAnySet() {
        Signature.SignatureOperator signature = Signature.of(Signature.ArgMatcher.ANY_SET);
        assertSameSignature(signature, ImmutableList.of(new SetType(DataTypes.LONG)));
        assertWrongSignature(signature, ImmutableList.of(DataTypes.STRING));
    }

    @Test
    public void testVarArgs() {
        Signature.SignatureOperator sig = Signature.of(true, false, Signature.ArgMatcher.STRING);
        assertSameSignature(sig, ImmutableList.of(DataTypes.STRING));
        assertSameSignature(sig, ImmutableList.of(DataTypes.STRING, DataTypes.STRING));
        assertSameSignature(sig, ImmutableList.of(DataTypes.STRING, DataTypes.STRING, DataTypes.STRING));
        assertWrongSignature(sig, ImmutableList.of(DataTypes.INTEGER));
        assertWrongSignature(sig, ImmutableList.of(DataTypes.STRING, DataTypes.LONG));
        assertWrongSignature(sig, ImmutableList.of(DataTypes.LONG, DataTypes.STRING, DataTypes.STRING));

        // nulls are replaced
        assertSignature(sig, ImmutableList.of(DataTypes.STRING, DataTypes.UNDEFINED, DataTypes.STRING),
            DataTypes.STRING, DataTypes.STRING, DataTypes.STRING);

        assertSignature(sig, ImmutableList.of(DataTypes.UNDEFINED, DataTypes.UNDEFINED),
            DataTypes.STRING, DataTypes.STRING);
    }

    @Test
    public void testVarArgsStrictTypes() {
        // matches varArgs of any, but need to be of same type
        Signature.SignatureOperator sig = Signature.of(true, true, Signature.ArgMatcher.ANY);

        assertSameSignature(sig, ImmutableList.of(DataTypes.STRING));
        assertSameSignature(sig, ImmutableList.of(DataTypes.STRING, DataTypes.STRING));

        assertWrongSignature(sig, ImmutableList.of(DataTypes.STRING, DataTypes.BOOLEAN));

        // nulls are replaced only if possible
        assertSignature(sig, ImmutableList.of(DataTypes.STRING, DataTypes.UNDEFINED, DataTypes.STRING),
            DataTypes.STRING, DataTypes.STRING, DataTypes.STRING);

        // if all args are null, then no guess can be made so the args are not modified
        assertSameSignature(sig, ImmutableList.of(DataTypes.UNDEFINED, DataTypes.UNDEFINED));
    }
}
