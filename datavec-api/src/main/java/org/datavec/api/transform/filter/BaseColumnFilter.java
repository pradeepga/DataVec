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

package org.datavec.api.transform.filter;

import org.datavec.api.writable.Writable;
import org.datavec.api.transform.schema.Schema;

import java.util.List;

/**Abstract class for filtering examples based on the values in a single column
 */
public abstract class BaseColumnFilter implements Filter {

    protected Schema schema;
    protected final String column;
    protected int columnIdx;

    protected BaseColumnFilter(String column){
        this.column = column;
    }

    @Override
    public boolean removeExample(List<Writable> writables) {
        return removeExample(writables.get(columnIdx));
    }

    @Override
    public boolean removeSequence(List<List<Writable>> sequence) {
        for(List<Writable> c : sequence){
            if(removeExample(c)) return true;
        }
        return false;
    }

    @Override
    public void setInputSchema(Schema schema) {
        this.schema = schema;
        this.columnIdx = schema.getIndexOfColumn(column);
    }

    /** Should the example or sequence be removed, based on the values from the specified column? */
    public abstract boolean removeExample(Writable writable);
}
