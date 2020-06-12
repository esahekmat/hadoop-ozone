/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.hadoop.ozone.om.helpers;

import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.KeyLocationList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A list of key locations. This class represents one single version of the
 * blocks of a key.
 */
public class OmKeyLocationInfoGroup {
  private final long version;
  private final Map<Long, List<OmKeyLocationInfo>> locationVersionList;

  public OmKeyLocationInfoGroup(long version,
                                List<OmKeyLocationInfo> locations) {
    this.version = version;
    this.locationVersionList = locations.stream()
        .collect(Collectors.groupingBy(OmKeyLocationInfo::getCreateVersion));
    //prevent NPE
    this.locationVersionList.putIfAbsent(version, new ArrayList<>());
  }

  public OmKeyLocationInfoGroup(long version,
                                Map<Long, List<OmKeyLocationInfo>> locations) {
    this.version = version;
    this.locationVersionList = locations;
    //prevent NPE
    this.locationVersionList.putIfAbsent(version, new ArrayList<>());
  }

  /**
   * Return only the blocks that are created in the most recent version.
   *
   * @return the list of blocks that are created in the latest version.
   */
  public List<OmKeyLocationInfo> getBlocksLatestVersionOnly() {
    return new ArrayList<>(locationVersionList.get(version));
  }

  public long getVersion() {
    return version;
  }

  public List<OmKeyLocationInfo> getLocationList() {
    return locationVersionList.values().stream().flatMap(List::stream)
        .collect(Collectors.toList());
  }

  public long getLocationListCount() {
    return locationVersionList.values().stream().mapToLong(List::size).sum();
  }

  public List<OmKeyLocationInfo> getLocationList(Long versionToFetch) {
    return new ArrayList<>(locationVersionList.get(versionToFetch));
  }

  public KeyLocationList getProtobuf() {
    return KeyLocationList.newBuilder()
        .setVersion(version)
        .addAllKeyLocations(
            locationVersionList.values().stream()
                .flatMap(List::stream)
                .map(OmKeyLocationInfo::getProtobuf)
                .collect(Collectors.toList()))
        .build();
  }

  public static OmKeyLocationInfoGroup getFromProtobuf(
      KeyLocationList keyLocationList) {
    return new OmKeyLocationInfoGroup(
        keyLocationList.getVersion(),
        keyLocationList.getKeyLocationsList().stream()
            .map(OmKeyLocationInfo::getFromProtobuf)
            .collect(Collectors.groupingBy(OmKeyLocationInfo::getCreateVersion))
    );
  }

  /**
   * Given a new block location, generate a new version list based upon this
   * one.
   *
   * @param newLocationList a list of new location to be added.
   * @return newly generated OmKeyLocationInfoGroup
   */
  OmKeyLocationInfoGroup generateNextVersion(
      List<OmKeyLocationInfo> newLocationList) {
    Map<Long, List<OmKeyLocationInfo>> newMap =
        new HashMap<>(locationVersionList);
    newMap.put(version + 1, new ArrayList<>(newLocationList));
    return new OmKeyLocationInfoGroup(version + 1, newMap);
  }

  void appendNewBlocks(List<OmKeyLocationInfo> newLocationList) {
    List<OmKeyLocationInfo> locationList = locationVersionList.get(version);
    for (OmKeyLocationInfo info : newLocationList) {
      info.setCreateVersion(version);
      locationList.add(info);
    }
  }

  void removeBlocks(long versionToRemove){
    locationVersionList.remove(versionToRemove);
  }

  void addAll(long versionToAdd, List<OmKeyLocationInfo> locationInfoList) {
    locationVersionList.putIfAbsent(versionToAdd, new ArrayList<>());
    List<OmKeyLocationInfo> list = locationVersionList.get(versionToAdd);
    list.addAll(locationInfoList);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("version:").append(version).append(" ");
    for (List<OmKeyLocationInfo> kliList : locationVersionList.values()) {
      for(OmKeyLocationInfo kli: kliList) {
        sb.append(kli.getLocalID()).append(" || ");
      }
    }
    return sb.toString();
  }
}
