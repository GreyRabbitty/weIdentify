/*
 *       Copyright© (2018-2020) WeBank Co., Ltd.
 *
 *       This file is part of weid-java-sdk.
 *
 *       weid-java-sdk is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weid-java-sdk is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weid-java-sdk.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.suite.auth.inf;

import java.util.List;

import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.suite.auth.protocol.WeIdAuthObj;

/**
 * Created by Junqi Zhang on 2020/3/8.
 */
public interface WeIdAuth {

    /**
     * 每次调用都是覆盖rule
     */
    public Integer setWhiteList(List<String> whitelistWeId);

    public Integer registerCallBack(WeIdAuthCallback callback);

    public WeIdAuthCallback getCallBack();

    public Integer addWeIdAuthObj(WeIdAuthObj weIdAuthObj);

    public WeIdAuthObj getWeIdAuthObjByChannelId(String channelId);

    /**
     * 如果使用amop需要传入orgId; 如果使用https，则传入url
     */
    public ResponseData<WeIdAuthObj> createAuthenticatedChannel(
        String toOrgId,
        WeIdAuthentication weIdAuthentication);

    public ResponseData<WeIdAuthObj> createMutualAuthenticatedChannel(
        String toOrgId,
        WeIdAuthentication weIdAuthentication);

//    void registerCallback(Integer directRouteMsgType, AmopCallback directRouteCallback);
}
