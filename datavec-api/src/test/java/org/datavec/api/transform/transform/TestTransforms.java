/*
 *  * Copyright 2016 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */

package org.datavec.api.transform.transform;

import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.LongWritable;
import org.datavec.api.transform.ColumnType;
import org.datavec.api.transform.condition.column.IntegerColumnCondition;
import org.datavec.api.transform.metadata.CategoricalMetaData;
import org.datavec.api.transform.metadata.DoubleMetaData;
import org.datavec.api.transform.metadata.IntegerMetaData;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.transform.categorical.CategoricalToIntegerTransform;
import org.datavec.api.transform.transform.categorical.IntegerToCategoricalTransform;
import org.datavec.api.transform.transform.column.DuplicateColumnsTransform;
import org.datavec.api.transform.transform.column.RemoveColumnsTransform;
import org.datavec.api.transform.transform.column.RenameColumnsTransform;
import org.datavec.api.transform.transform.column.ReorderColumnsTransform;
import org.datavec.api.transform.transform.condition.ConditionalReplaceValueTransform;
import org.datavec.api.transform.transform.integer.ReplaceEmptyIntegerWithValueTransform;
import org.datavec.api.transform.transform.integer.ReplaceInvalidWithIntegerTransform;
import org.datavec.api.transform.transform.longtransform.LongColumnsMathOpTransform;
import org.datavec.api.transform.transform.longtransform.LongMathOpTransform;
import org.datavec.api.transform.transform.doubletransform.*;
import org.datavec.api.transform.transform.string.*;
import org.datavec.api.transform.transform.time.DeriveColumnsFromTimeTransform;
import org.datavec.api.transform.transform.time.StringToTimeTransform;
import org.datavec.api.transform.transform.time.TimeMathOpTransform;
import org.datavec.api.transform.MathOp;
import org.datavec.api.transform.Transform;
import org.datavec.api.transform.condition.Condition;
import org.datavec.api.transform.condition.ConditionOp;
import org.datavec.api.transform.condition.column.StringColumnCondition;
import org.datavec.api.transform.metadata.LongMetaData;
import org.datavec.api.transform.transform.categorical.CategoricalToOneHotTransform;
import org.datavec.api.transform.transform.categorical.StringToCategoricalTransform;
import org.datavec.api.transform.transform.condition.ConditionalCopyValueTransform;
import org.datavec.api.transform.transform.integer.IntegerColumnsMathOpTransform;
import org.datavec.api.transform.transform.integer.IntegerMathOpTransform;
import junit.framework.TestCase;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by Alex on 21/03/2016.
 */
public class TestTransforms {

    public static Schema getSchema(ColumnType type, String... colNames) {

        Schema.Builder schema = new Schema.Builder();

        switch (type) {
            case String:
                schema.addColumnString("column");
                break;
            case Integer:
                schema.addColumnInteger("column");
                break;
            case Long:
                schema.addColumnLong("column");
                break;
            case Double:
                schema.addColumnDouble("column");
                break;
            case Categorical:
                schema.addColumnCategorical("column", colNames);
                break;
            case Time:
                schema.addColumnTime("column",DateTimeZone.UTC);
                break;
            default:
                throw new RuntimeException();
        }
        return schema.build();
    }

    @Test
    public void testCategoricalToInteger() {
        Schema schema = getSchema(ColumnType.Categorical, "zero", "one", "two");

        Transform transform = new CategoricalToIntegerTransform("column");
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);


        TestCase.assertEquals(ColumnType.Integer, out.getMetaData(0).getColumnType());
        IntegerMetaData meta = (IntegerMetaData) out.getMetaData(0);
        assertNotNull(meta.getMinAllowedValue());
        assertEquals(0, (int) meta.getMinAllowedValue());

        assertNotNull(meta.getMaxAllowedValue());
        assertEquals(2, (int) meta.getMaxAllowedValue());

        assertEquals(0, transform.map(Collections.singletonList((Writable) new Text("zero"))).get(0).toInt());
        assertEquals(1, transform.map(Collections.singletonList((Writable) new Text("one"))).get(0).toInt());
        assertEquals(2, transform.map(Collections.singletonList((Writable) new Text("two"))).get(0).toInt());
    }

    @Test
    public void testCategoricalToOneHotTransform() {
        Schema schema = getSchema(ColumnType.Categorical, "zero", "one", "two");

        Transform transform = new CategoricalToOneHotTransform("column");
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(3, out.getColumnMetaData().size());
        for (int i = 0; i < 3; i++) {
            TestCase.assertEquals(ColumnType.Integer, out.getMetaData(i).getColumnType());
            IntegerMetaData meta = (IntegerMetaData) out.getMetaData(i);
            assertNotNull(meta.getMinAllowedValue());
            assertEquals(0, (int) meta.getMinAllowedValue());

            assertNotNull(meta.getMaxAllowedValue());
            assertEquals(1, (int) meta.getMaxAllowedValue());
        }

        assertEquals(Arrays.asList(new IntWritable(1), new IntWritable(0), new IntWritable(0)),
                transform.map(Collections.singletonList((Writable) new Text("zero"))));
        assertEquals(Arrays.asList(new IntWritable(0), new IntWritable(1), new IntWritable(0)),
                transform.map(Collections.singletonList((Writable) new Text("one"))));
        assertEquals(Arrays.asList(new IntWritable(0), new IntWritable(0), new IntWritable(1)),
                transform.map(Collections.singletonList((Writable) new Text("two"))));
    }

    @Test
    public void testIntegerToCategoricalTransform() {
        Schema schema = getSchema(ColumnType.Integer);

        Transform transform = new IntegerToCategoricalTransform("column", Arrays.asList("zero", "one", "two"));
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Categorical, out.getMetaData(0).getColumnType());
        CategoricalMetaData meta = (CategoricalMetaData) out.getMetaData(0);
        assertEquals(Arrays.asList("zero", "one", "two"), meta.getStateNames());

        assertEquals(Collections.singletonList((Writable) new Text("zero")),
                transform.map(Collections.singletonList((Writable) new IntWritable(0))));
        assertEquals(Collections.singletonList((Writable) new Text("one")),
                transform.map(Collections.singletonList((Writable) new IntWritable(1))));
        assertEquals(Collections.singletonList((Writable) new Text("two")),
                transform.map(Collections.singletonList((Writable) new IntWritable(2))));
    }

    @Test
    public void testStringToCategoricalTransform() {
        Schema schema = getSchema(ColumnType.String);

        Transform transform = new StringToCategoricalTransform("column", Arrays.asList("zero", "one", "two"));
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Categorical, out.getMetaData(0).getColumnType());
        CategoricalMetaData meta = (CategoricalMetaData) out.getMetaData(0);
        assertEquals(Arrays.asList("zero", "one", "two"), meta.getStateNames());

        assertEquals(Collections.singletonList((Writable) new Text("zero")),
                transform.map(Collections.singletonList((Writable) new Text("zero"))));
        assertEquals(Collections.singletonList((Writable) new Text("one")),
                transform.map(Collections.singletonList((Writable) new Text("one"))));
        assertEquals(Collections.singletonList((Writable) new Text("two")),
                transform.map(Collections.singletonList((Writable) new Text("two"))));
    }

    @Test
    public void testRemoveColumnsTransform() {
        Schema schema = new Schema.Builder()
                .addColumnDouble("first")
                .addColumnString("second")
                .addColumnInteger("third")
                .addColumnLong("fourth")
                .build();

        Transform transform = new RemoveColumnsTransform("first", "fourth");
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);

        assertEquals(2, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.String, out.getMetaData(0).getColumnType());
        TestCase.assertEquals(ColumnType.Integer, out.getMetaData(1).getColumnType());

        assertEquals(Arrays.asList(new Text("one"), new IntWritable(1)),
                transform.map(Arrays.asList((Writable) new DoubleWritable(1.0), new Text("one"), new IntWritable(1), new LongWritable(1L))));
    }

    @Test
    public void testReplaceEmptyIntegerWithValueTransform() {
        Schema schema = getSchema(ColumnType.Integer);

        Transform transform = new ReplaceEmptyIntegerWithValueTransform("column", 1000);
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Integer, out.getMetaData(0).getColumnType());

        assertEquals(Collections.singletonList((Writable) new IntWritable(0)),
                transform.map(Collections.singletonList((Writable) new IntWritable(0))));
        assertEquals(Collections.singletonList((Writable) new IntWritable(1)),
                transform.map(Collections.singletonList((Writable) new IntWritable(1))));
        assertEquals(Collections.singletonList((Writable) new IntWritable(1000)),
                transform.map(Collections.singletonList((Writable) new Text(""))));
    }

    @Test
    public void testReplaceInvalidWithIntegerTransform() {
        Schema schema = getSchema(ColumnType.Integer);

        Transform transform = new ReplaceInvalidWithIntegerTransform("column", 1000);
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Integer, out.getMetaData(0).getColumnType());

        assertEquals(Collections.singletonList((Writable) new IntWritable(0)),
                transform.map(Collections.singletonList((Writable) new IntWritable(0))));
        assertEquals(Collections.singletonList((Writable) new IntWritable(1)),
                transform.map(Collections.singletonList((Writable) new IntWritable(1))));
        assertEquals(Collections.singletonList((Writable) new IntWritable(1000)),
                transform.map(Collections.singletonList((Writable) new Text(""))));
    }

    @Test
    public void testLog2Normalizer() {
        Schema schema = getSchema(ColumnType.Double);

        double mu = 2.0;
        double min = 1.0;
        double scale = 0.5;

        Transform transform = new Log2Normalizer("column", mu, min, scale);
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Double, out.getMetaData(0).getColumnType());
        DoubleMetaData meta = (DoubleMetaData) out.getMetaData(0);
        assertNotNull(meta.getMinAllowedValue());
        assertEquals(0, meta.getMinAllowedValue(), 1e-6);
        assertNull(meta.getMaxAllowedValue());

        double loge2 = Math.log(2);
        assertEquals(0.0, transform.map(Collections.singletonList((Writable) new DoubleWritable(min))).get(0).toDouble(), 1e-6);
        double d = scale * Math.log((10 - min) / (mu - min) + 1) / loge2;
        assertEquals(d, transform.map(Collections.singletonList((Writable) new DoubleWritable(10))).get(0).toDouble(), 1e-6);
        d = scale * Math.log((3 - min) / (mu - min) + 1) / loge2;
        assertEquals(d, transform.map(Collections.singletonList((Writable) new DoubleWritable(3))).get(0).toDouble(), 1e-6);
    }

    @Test
    public void testDoubleMinMaxNormalizerTransform() {
        Schema schema = getSchema(ColumnType.Double);

        Transform transform = new MinMaxNormalizer("column", 0, 100);
        Transform transform2 = new MinMaxNormalizer("column", 0, 100, -1, 1);
        transform.setInputSchema(schema);
        transform2.setInputSchema(schema);

        Schema out = transform.transform(schema);
        Schema out2 = transform2.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Double, out.getMetaData(0).getColumnType());
        DoubleMetaData meta = (DoubleMetaData) out.getMetaData(0);
        DoubleMetaData meta2 = (DoubleMetaData) out2.getMetaData(0);
        assertEquals(0, meta.getMinAllowedValue(), 1e-6);
        assertEquals(1, meta.getMaxAllowedValue(), 1e-6);
        assertEquals(-1, meta2.getMinAllowedValue(), 1e-6);
        assertEquals(1, meta2.getMaxAllowedValue(), 1e-6);


        assertEquals(0.0, transform.map(Collections.singletonList((Writable) new DoubleWritable(0))).get(0).toDouble(), 1e-6);
        assertEquals(1.0, transform.map(Collections.singletonList((Writable) new DoubleWritable(100))).get(0).toDouble(), 1e-6);
        assertEquals(0.5, transform.map(Collections.singletonList((Writable) new DoubleWritable(50))).get(0).toDouble(), 1e-6);

        assertEquals(-1.0, transform2.map(Collections.singletonList((Writable) new DoubleWritable(0))).get(0).toDouble(), 1e-6);
        assertEquals(1.0, transform2.map(Collections.singletonList((Writable) new DoubleWritable(100))).get(0).toDouble(), 1e-6);
        assertEquals(0.0, transform2.map(Collections.singletonList((Writable) new DoubleWritable(50))).get(0).toDouble(), 1e-6);
    }

    @Test
    public void testStandardizeNormalizer() {
        Schema schema = getSchema(ColumnType.Double);

        double mu = 1.0;
        double sigma = 2.0;

        Transform transform = new StandardizeNormalizer("column", mu, sigma);
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Double, out.getMetaData(0).getColumnType());
        DoubleMetaData meta = (DoubleMetaData) out.getMetaData(0);
        assertNull(meta.getMinAllowedValue());
        assertNull(meta.getMaxAllowedValue());


        assertEquals(0.0, transform.map(Collections.singletonList((Writable) new DoubleWritable(mu))).get(0).toDouble(), 1e-6);
        double d = (10 - mu) / sigma;
        assertEquals(d, transform.map(Collections.singletonList((Writable) new DoubleWritable(10))).get(0).toDouble(), 1e-6);
        d = (-2 - mu) / sigma;
        assertEquals(d, transform.map(Collections.singletonList((Writable) new DoubleWritable(-2))).get(0).toDouble(), 1e-6);
    }

    @Test
    public void testSubtractMeanNormalizer() {
        Schema schema = getSchema(ColumnType.Double);

        double mu = 1.0;

        Transform transform = new SubtractMeanNormalizer("column", mu);
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Double, out.getMetaData(0).getColumnType());
        DoubleMetaData meta = (DoubleMetaData) out.getMetaData(0);
        assertNull(meta.getMinAllowedValue());
        assertNull(meta.getMaxAllowedValue());


        assertEquals(0.0, transform.map(Collections.singletonList((Writable) new DoubleWritable(mu))).get(0).toDouble(), 1e-6);
        assertEquals(10 - mu, transform.map(Collections.singletonList((Writable) new DoubleWritable(10))).get(0).toDouble(), 1e-6);
    }

    @Test
    public void testMapAllStringsExceptListTransform() {
        Schema schema = getSchema(ColumnType.String);

        Transform transform = new MapAllStringsExceptListTransform("column", "replacement", Arrays.asList("one", "two", "three"));
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.String, out.getMetaData(0).getColumnType());

        assertEquals(Collections.singletonList((Writable) new Text("one")),
                transform.map(Collections.singletonList((Writable) new Text("one"))));
        assertEquals(Collections.singletonList((Writable) new Text("two")),
                transform.map(Collections.singletonList((Writable) new Text("two"))));
        assertEquals(Collections.singletonList((Writable) new Text("replacement")),
                transform.map(Collections.singletonList((Writable) new Text("this should be replaced"))));
    }

    @Test
    public void testRemoveWhitespaceTransform() {
        Schema schema = getSchema(ColumnType.String);

        Transform transform = new RemoveWhiteSpaceTransform("column");
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.String, out.getMetaData(0).getColumnType());

        assertEquals(Collections.singletonList((Writable) new Text("one")),
                transform.map(Collections.singletonList((Writable) new Text("one "))));
        assertEquals(Collections.singletonList((Writable) new Text("two")),
                transform.map(Collections.singletonList((Writable) new Text("two\t"))));
        assertEquals(Collections.singletonList((Writable) new Text("three")),
                transform.map(Collections.singletonList((Writable) new Text("three\n"))));
        assertEquals(Collections.singletonList((Writable) new Text("one")),
                transform.map(Collections.singletonList((Writable) new Text(" o n e\t"))));
    }

    @Test
    public void testReplaceEmptyStringTransform() {
        Schema schema = getSchema(ColumnType.String);

        Transform transform = new ReplaceEmptyStringTransform("column", "newvalue");
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.String, out.getMetaData(0).getColumnType());

        assertEquals(Collections.singletonList((Writable) new Text("one")),
                transform.map(Collections.singletonList((Writable) new Text("one"))));
        assertEquals(Collections.singletonList((Writable) new Text("newvalue")),
                transform.map(Collections.singletonList((Writable) new Text(""))));
        assertEquals(Collections.singletonList((Writable) new Text("three")),
                transform.map(Collections.singletonList((Writable) new Text("three"))));
    }

    @Test
    public void testStringListToCategoricalSetTransform() {
        //Idea: String list to a set of categories... "a,c" for categories {a,b,c} -> "true","false","true"

        Schema schema = getSchema(ColumnType.String);

        Transform transform = new StringListToCategoricalSetTransform("column", Arrays.asList("a", "b", "c"), Arrays.asList("a", "b", "c"), ",");
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);
        assertEquals(3, out.getColumnMetaData().size());
        for (int i = 0; i < 3; i++) {
            TestCase.assertEquals(ColumnType.Categorical, out.getType(i));
            CategoricalMetaData meta = (CategoricalMetaData) out.getMetaData(i);
            assertEquals(Arrays.asList("true", "false"), meta.getStateNames());
        }

        assertEquals(Arrays.asList(new Text("false"), new Text("false"), new Text("false")), transform.map(Collections.singletonList((Writable) new Text(""))));
        assertEquals(Arrays.asList(new Text("true"), new Text("false"), new Text("false")), transform.map(Collections.singletonList((Writable) new Text("a"))));
        assertEquals(Arrays.asList(new Text("false"), new Text("true"), new Text("false")), transform.map(Collections.singletonList((Writable) new Text("b"))));
        assertEquals(Arrays.asList(new Text("false"), new Text("false"), new Text("true")), transform.map(Collections.singletonList((Writable) new Text("c"))));
        assertEquals(Arrays.asList(new Text("true"), new Text("false"), new Text("true")), transform.map(Collections.singletonList((Writable) new Text("a,c"))));
        assertEquals(Arrays.asList(new Text("true"), new Text("true"), new Text("true")), transform.map(Collections.singletonList((Writable) new Text("a,b,c"))));
    }

    @Test
    public void testStringMapTransform() {
        Schema schema = getSchema(ColumnType.String);

        Map<String, String> map = new HashMap<>();
        map.put("one", "ONE");
        map.put("two", "TWO");
        Transform transform = new StringMapTransform("column", map);
        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.String, out.getMetaData(0).getColumnType());

        assertEquals(Collections.singletonList((Writable) new Text("ONE")),
                transform.map(Collections.singletonList((Writable) new Text("one"))));
        assertEquals(Collections.singletonList((Writable) new Text("TWO")),
                transform.map(Collections.singletonList((Writable) new Text("two"))));
        assertEquals(Collections.singletonList((Writable) new Text("three")),
                transform.map(Collections.singletonList((Writable) new Text("three"))));
    }


    @Test
    public void testStringToTimeTransform() throws Exception {
        Schema schema = getSchema(ColumnType.String);

        //http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html
        Transform transform = new StringToTimeTransform("column", "YYYY-MM-dd HH:mm:ss", DateTimeZone.forID("UTC"));
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);

        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Time, out.getMetaData(0).getColumnType());

        String in1 = "2016-01-01 12:30:45";
        long out1 = 1451651445000L;

        String in2 = "2015-06-30 23:59:59";
        long out2 = 1435708799000L;

        assertEquals(Collections.singletonList((Writable) new LongWritable(out1)),
                transform.map(Collections.singletonList((Writable) new Text(in1))));
        assertEquals(Collections.singletonList((Writable) new LongWritable(out2)),
                transform.map(Collections.singletonList((Writable) new Text(in2))));

        //Check serialization: things like DateTimeFormatter etc aren't serializable, hence we need custom serialization :/
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(transform);

        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);

        Transform deserialized = (Transform) ois.readObject();
        assertEquals(Collections.singletonList((Writable) new LongWritable(out1)),
                deserialized.map(Collections.singletonList((Writable) new Text(in1))));
        assertEquals(Collections.singletonList((Writable) new LongWritable(out2)),
                deserialized.map(Collections.singletonList((Writable) new Text(in2))));
    }

    @Test
    public void testDeriveColumnsFromTimeTransform() throws Exception {
        Schema schema = new Schema.Builder()
                .addColumnTime("column", DateTimeZone.forID("UTC"))
                .addColumnString("otherColumn")
                .build();

        Transform transform = new DeriveColumnsFromTimeTransform.Builder("column")
                .insertAfter("otherColumn")
                .addIntegerDerivedColumn("hour", DateTimeFieldType.hourOfDay())
                .addIntegerDerivedColumn("day", DateTimeFieldType.dayOfMonth())
                .addIntegerDerivedColumn("second", DateTimeFieldType.secondOfMinute())
                .addStringDerivedColumn("humanReadable","YYYY-MM-dd HH:mm:ss",DateTimeZone.UTC)
                .build();

        transform.setInputSchema(schema);
        Schema out = transform.transform(schema);

        assertEquals(6, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Time, out.getMetaData(0).getColumnType());
        TestCase.assertEquals(ColumnType.String, out.getMetaData(1).getColumnType());
        TestCase.assertEquals(ColumnType.Integer, out.getMetaData(2).getColumnType());
        TestCase.assertEquals(ColumnType.Integer, out.getMetaData(3).getColumnType());
        TestCase.assertEquals(ColumnType.Integer, out.getMetaData(4).getColumnType());
        TestCase.assertEquals(ColumnType.String, out.getMetaData(5).getColumnType());

        assertEquals("column", out.getName(0));
        assertEquals("otherColumn", out.getName(1));
        assertEquals("hour", out.getName(2));
        assertEquals("day", out.getName(3));
        assertEquals("second", out.getName(4));
        assertEquals("humanReadable", out.getName(5));

        long in1 = 1451651445000L;      //"2016-01-01 12:30:45" GMT

        List<Writable> out1 = new ArrayList<>();
        out1.add(new LongWritable(in1));
        out1.add(new Text("otherColumnValue"));
        out1.add(new IntWritable(12));  //hour
        out1.add(new IntWritable(1));   //day
        out1.add(new IntWritable(45));  //second
        out1.add(new Text("2016-01-01 12:30:45"));

        long in2 = 1435708799000L;      //"2015-06-30 23:59:59" GMT
        List<Writable> out2 = new ArrayList<>();
        out2.add(new LongWritable(in2));
        out2.add(new Text("otherColumnValue"));
        out2.add(new IntWritable(23));  //hour
        out2.add(new IntWritable(30));   //day
        out2.add(new IntWritable(59));  //second
        out2.add(new Text("2015-06-30 23:59:59"));

        assertEquals(out1, transform.map(Arrays.asList((Writable) new LongWritable(in1), new Text("otherColumnValue"))));
        assertEquals(out2, transform.map(Arrays.asList((Writable) new LongWritable(in2), new Text("otherColumnValue"))));



        //Check serialization: things like DateTimeFormatter etc aren't serializable, hence we need custom serialization :/
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(transform);

        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);

        Transform deserialized = (Transform) ois.readObject();
        assertEquals(out1, deserialized.map(Arrays.asList((Writable) new LongWritable(in1), new Text("otherColumnValue"))));
        assertEquals(out2, deserialized.map(Arrays.asList((Writable) new LongWritable(in2), new Text("otherColumnValue"))));
    }


    @Test
    public void testDuplicateColumnsTransform() {

        Schema schema = new Schema.Builder()
                .addColumnString("stringCol")
                .addColumnInteger("intCol")
                .addColumnLong("longCol")
                .build();

        List<String> toDup = Arrays.asList("intCol", "longCol");
        List<String> newNames = Arrays.asList("dup_intCol", "dup_longCol");

        Transform transform = new DuplicateColumnsTransform(toDup, newNames);
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);
        assertEquals(5, out.getColumnMetaData().size());

        List<String> expOutNames = Arrays.asList("stringCol", "intCol", "dup_intCol", "longCol", "dup_longCol");
        List<ColumnType> expOutTypes = Arrays.asList(ColumnType.String, ColumnType.Integer, ColumnType.Integer, ColumnType.Long, ColumnType.Long);
        for (int i = 0; i < 5; i++) {
            assertEquals(expOutNames.get(i), out.getName(i));
            TestCase.assertEquals(expOutTypes.get(i), out.getType(i));
        }

        List<Writable> inList = Arrays.asList((Writable) new Text("one"), new IntWritable(2), new LongWritable(3L));
        List<Writable> outList = Arrays.asList((Writable) new Text("one"), new IntWritable(2), new IntWritable(2), new LongWritable(3L), new LongWritable(3L));

        assertEquals(outList, transform.map(inList));
    }

    @Test
    public void testIntegerMathOpTransform() {
        Schema schema = new Schema.Builder()
                .addColumnInteger("column", -1, 1)
                .build();

        Transform transform = new IntegerMathOpTransform("column", MathOp.Multiply, 5);
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);
        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Integer, out.getType(0));
        IntegerMetaData meta = (IntegerMetaData) out.getMetaData(0);
        assertEquals(-5, (int) meta.getMinAllowedValue());
        assertEquals(5, (int) meta.getMaxAllowedValue());

        assertEquals(Collections.singletonList((Writable) new IntWritable(-5)), transform.map(Collections.singletonList((Writable) new IntWritable(-1))));
        assertEquals(Collections.singletonList((Writable) new IntWritable(0)), transform.map(Collections.singletonList((Writable) new IntWritable(0))));
        assertEquals(Collections.singletonList((Writable) new IntWritable(5)), transform.map(Collections.singletonList((Writable) new IntWritable(1))));
    }

    @Test
    public void testIntegerColumnsMathOpTransform(){
        Schema schema = new Schema.Builder()
                .addColumnInteger("first")
                .addColumnString("second")
                .addColumnInteger("third")
                .build();

        Transform transform = new IntegerColumnsMathOpTransform("out",MathOp.Add,"first","third");
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);
        assertEquals(4, out.numColumns());
        assertEquals(Arrays.asList("first","second","third","out"), out.getColumnNames());
        assertEquals(Arrays.asList(ColumnType.Integer, ColumnType.String, ColumnType.Integer, ColumnType.Integer), out.getColumnTypes());


        assertEquals(Arrays.asList((Writable)new IntWritable(1),new Text("something"), new IntWritable(2), new IntWritable(3)),
                transform.map(Arrays.asList((Writable)new IntWritable(1),new Text("something"), new IntWritable(2))));
        assertEquals(Arrays.asList((Writable)new IntWritable(100),new Text("something2"), new IntWritable(21), new IntWritable(121)),
                transform.map(Arrays.asList((Writable)new IntWritable(100),new Text("something2"), new IntWritable(21))));
    }

    @Test
    public void testLongMathOpTransform() {
        Schema schema = new Schema.Builder()
                .addColumnLong("column", -1L, 1L)
                .build();

        Transform transform = new LongMathOpTransform("column", MathOp.Multiply, 5);
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);
        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Long, out.getType(0));
        LongMetaData meta = (LongMetaData) out.getMetaData(0);
        assertEquals(-5, (long) meta.getMinAllowedValue());
        assertEquals(5, (long) meta.getMaxAllowedValue());

        assertEquals(Collections.singletonList((Writable) new LongWritable(-5)), transform.map(Collections.singletonList((Writable) new LongWritable(-1))));
        assertEquals(Collections.singletonList((Writable) new LongWritable(0)), transform.map(Collections.singletonList((Writable) new LongWritable(0))));
        assertEquals(Collections.singletonList((Writable) new LongWritable(5)), transform.map(Collections.singletonList((Writable) new LongWritable(1))));
    }

    @Test
    public void testLongColumnsMathOpTransform(){
        Schema schema = new Schema.Builder()
                .addColumnLong("first")
                .addColumnString("second")
                .addColumnLong("third")
                .build();

        Transform transform = new LongColumnsMathOpTransform("out",MathOp.Add,"first","third");
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);
        assertEquals(4, out.numColumns());
        assertEquals(Arrays.asList("first","second","third","out"), out.getColumnNames());
        assertEquals(Arrays.asList(ColumnType.Long, ColumnType.String, ColumnType.Long, ColumnType.Long), out.getColumnTypes());


        assertEquals(Arrays.asList((Writable)new LongWritable(1),new Text("something"), new LongWritable(2), new LongWritable(3)),
                transform.map(Arrays.asList((Writable)new LongWritable(1),new Text("something"), new LongWritable(2))));
        assertEquals(Arrays.asList((Writable)new LongWritable(100),new Text("something2"), new LongWritable(21), new LongWritable(121)),
                transform.map(Arrays.asList((Writable)new LongWritable(100),new Text("something2"), new LongWritable(21))));
    }

    @Test
    public void testTimeMathOpTransform() {
        Schema schema = new Schema.Builder()
                .addColumnTime("column", DateTimeZone.UTC)
                .build();

        Transform transform = new TimeMathOpTransform("column", MathOp.Add, 12, TimeUnit.HOURS);    //12 hours: 43200000 milliseconds
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);
        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Time, out.getType(0));

        assertEquals(Collections.singletonList((Writable) new LongWritable(1000+43200000)), transform.map(Collections.singletonList((Writable) new LongWritable(1000))));
        assertEquals(Collections.singletonList((Writable) new LongWritable(1452441600000L+43200000)), transform.map(Collections.singletonList((Writable) new LongWritable(1452441600000L))));
    }

    @Test
    public void testDoubleMathOpTransform() {
        Schema schema = new Schema.Builder()
                .addColumnDouble("column", -1.0, 1.0)
                .build();

        Transform transform = new DoubleMathOpTransform("column", MathOp.Multiply, 5.0);
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);
        assertEquals(1, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Double, out.getType(0));
        DoubleMetaData meta = (DoubleMetaData) out.getMetaData(0);
        assertEquals(-5.0, meta.getMinAllowedValue(), 1e-6);
        assertEquals(5.0, meta.getMaxAllowedValue(), 1e-6);

        assertEquals(Collections.singletonList((Writable) new DoubleWritable(-5)), transform.map(Collections.singletonList((Writable) new DoubleWritable(-1))));
        assertEquals(Collections.singletonList((Writable) new DoubleWritable(0)), transform.map(Collections.singletonList((Writable) new DoubleWritable(0))));
        assertEquals(Collections.singletonList((Writable) new DoubleWritable(5)), transform.map(Collections.singletonList((Writable) new DoubleWritable(1))));
    }

    @Test
    public void testDoubleColumnsMathOpTransform(){
        Schema schema = new Schema.Builder()
                .addColumnString("first")
                .addColumnDouble("second")
                .addColumnDouble("third")
                .build();

        Transform transform = new DoubleColumnsMathOpTransform("out",MathOp.Add,"second","third");
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);
        assertEquals(4, out.numColumns());
        assertEquals(Arrays.asList("first","second","third","out"), out.getColumnNames());
        assertEquals(Arrays.asList(ColumnType.String, ColumnType.Double, ColumnType.Double, ColumnType.Double), out.getColumnTypes());


        assertEquals(Arrays.asList((Writable)new Text("something"), new DoubleWritable(1.0), new DoubleWritable(2.1), new DoubleWritable(3.1)),
                transform.map(Arrays.asList((Writable)new Text("something"), new DoubleWritable(1.0), new DoubleWritable(2.1))));
        assertEquals(Arrays.asList((Writable)new Text("something2"), new DoubleWritable(100.0), new DoubleWritable(21.1), new DoubleWritable(121.1)),
                transform.map(Arrays.asList((Writable)new Text("something2"), new DoubleWritable(100.0), new DoubleWritable(21.1))));
    }

    @Test
    public void testRenameColumnsTransform() {

        Schema schema = new Schema.Builder()
                .addColumnDouble("col1")
                .addColumnString("col2")
                .addColumnInteger("col3")
                .build();

        Transform transform = new RenameColumnsTransform(Arrays.asList("col1", "col3"), Arrays.asList("column1", "column3"));
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);

        assertEquals(3, out.getColumnMetaData().size());
        TestCase.assertEquals(ColumnType.Double, out.getMetaData(0).getColumnType());
        TestCase.assertEquals(ColumnType.String, out.getMetaData(1).getColumnType());
        TestCase.assertEquals(ColumnType.Integer, out.getMetaData(2).getColumnType());

        assertEquals("column1", out.getName(0));
        assertEquals("col2", out.getName(1));
        assertEquals("column3", out.getName(2));
    }

    @Test
    public void testReorderColumnsTransform(){
        Schema schema = new Schema.Builder()
                .addColumnDouble("col1")
                .addColumnString("col2")
                .addColumnInteger("col3")
                .build();

        Transform transform = new ReorderColumnsTransform("col3","col2");
        transform.setInputSchema(schema);

        Schema out = transform.transform(schema);

        assertEquals(3, out.numColumns());
        assertEquals(Arrays.asList("col3","col2","col1"), out.getColumnNames());
        assertEquals(Arrays.asList(ColumnType.Integer, ColumnType.String, ColumnType.Double), out.getColumnTypes());

        assertEquals(Arrays.asList((Writable)new IntWritable(1), new Text("one"), new DoubleWritable(1.1)),
                transform.map(Arrays.asList((Writable)new DoubleWritable(1.1), new Text("one"), new IntWritable(1))));

        assertEquals(Arrays.asList((Writable)new IntWritable(2), new Text("two"), new DoubleWritable(200.2)),
                transform.map(Arrays.asList((Writable)new DoubleWritable(200.2), new Text("two"), new IntWritable(2))));
    }

    @Test
    public void testConditionalReplaceValueTransform() {
        Schema schema = getSchema(ColumnType.Integer);

        Condition condition = new IntegerColumnCondition("column", ConditionOp.LessThan, 0);
        condition.setInputSchema(schema);

        Transform transform = new ConditionalReplaceValueTransform("column",new IntWritable(0), condition);
        transform.setInputSchema(schema);

        assertEquals(Collections.singletonList((Writable)new IntWritable(10)),
                transform.map(Collections.singletonList((Writable)new IntWritable(10))));
        assertEquals(Collections.singletonList((Writable)new IntWritable(1)),
                transform.map(Collections.singletonList((Writable)new IntWritable(1))));
        assertEquals(Collections.singletonList((Writable)new IntWritable(0)),
                transform.map(Collections.singletonList((Writable)new IntWritable(0))));
        assertEquals(Collections.singletonList((Writable)new IntWritable(0)),
                transform.map(Collections.singletonList((Writable)new IntWritable(-1))));
        assertEquals(Collections.singletonList((Writable)new IntWritable(0)),
                transform.map(Collections.singletonList((Writable)new IntWritable(-10))));
    }

    @Test
    public void testConditionalCopyValueTransform(){
        Schema schema = new Schema.Builder()
                .addColumnsString("first","second","third")
                .build();

        Condition condition = new StringColumnCondition("third",ConditionOp.Equal,"");
        Transform transform = new ConditionalCopyValueTransform("third","second",condition);
        transform.setInputSchema(schema);

        List<Writable> list = Arrays.asList((Writable)new Text("first"), new Text("second"), new Text("third"));
        assertEquals(list,transform.map(list));

        list = Arrays.asList((Writable)new Text("first"), new Text("second"), new Text(""));
        List<Writable> exp = Arrays.asList((Writable)new Text("first"), new Text("second"), new Text("second"));
        assertEquals(exp, transform.map(list));
    }

}
