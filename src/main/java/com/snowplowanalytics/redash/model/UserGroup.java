/*
 * Copyright (c) 2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.redash.model;

import java.util.Objects;

public class UserGroup extends BaseEntity{

    public UserGroup(String name) {
        super(name);
    }

    public UserGroup(String name, int id) {
        super(name, id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UserGroup)) {
            return false;
        }
        UserGroup o1 = (UserGroup) o;
        return getId() == o1.getId() &&
                Objects.equals(getName(), o1.getName());
    }

}
