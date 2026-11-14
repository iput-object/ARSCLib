/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.dex.ins;

import com.reandroid.dex.id.StringId;
import com.reandroid.dex.key.StringKey;

public class InsConstString extends Ins21c implements ConstString{

    public InsConstString() {
        super(Opcode.CONST_STRING);
    }

    @Override
    public String getString(){
        StringId stringId = getStringId();
        if(stringId != null){
            return stringId.getString();
        }
        return null;
    }
    @Override
    public void setString(StringKey key) {
        super.setKey(key);
    }

    @Override
    public int getRegister() {
        return getRegister(0);
    }
    @Override
    public void setRegister(int register) {
        setRegister(0, register);
    }
    public StringId getStringId(){
        return getSectionId();
    }
    @Override
    public StringId getSectionId() {
        return (StringId) super.getSectionId();
    }
    @Override
    public StringKey getKey() {
        return (StringKey) super.getKey();
    }

    public InsConstStringJumbo toConstStringJumbo(){
        if(!needsConvertToConstStringJumbo()){
            return null;
        }
        StringId stringId = getSectionId();
        InsConstStringJumbo stringJumbo = Opcode.CONST_STRING_JUMBO.newInstance();
        stringJumbo.setRegister(getRegister());
        this.replace(stringJumbo);
        stringJumbo.setSectionId(stringId);
        return stringJumbo;
    }

    public boolean needsConvertToConstStringJumbo(){
        StringId stringId = getSectionId();
        if(stringId == null){
            return false;
        }
        int index = stringId.getIndex();
        return index != (index & 0xffff);
    }
}
