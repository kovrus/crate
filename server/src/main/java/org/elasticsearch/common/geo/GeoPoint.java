/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.geo;

import org.apache.lucene.util.BitUtil;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.ToXContentFragment;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

import static org.elasticsearch.common.geo.GeoHashUtils.mortonEncode;
import static org.elasticsearch.common.geo.GeoHashUtils.stringEncode;

public final class GeoPoint implements ToXContentFragment {

    private double lat;
    private double lon;

    public GeoPoint() {
    }

    /**
     * Create a new Geopoint from a string. This String must either be a geohash
     * or a lat-lon tuple.
     *
     * @param value String to create the point from
     */
    public GeoPoint(String value) {
        this.resetFromString(value);
    }

    public GeoPoint(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public GeoPoint(GeoPoint template) {
        this(template.getLat(), template.getLon());
    }

    public GeoPoint reset(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        return this;
    }

    public GeoPoint resetFromString(String value) {
        if (value.contains(",")) {
            String[] vals = value.split(",");
            if (vals.length > 3) {
                throw new ElasticsearchParseException("failed to parse [{}], expected 2 or 3 coordinates "
                    + "but found: [{}]", vals.length);
            }
            double lat = Double.parseDouble(vals[0].trim());
            double lon = Double.parseDouble(vals[1].trim());
            if (vals.length > 2) {
                GeoPoint.assertZValue(Double.parseDouble(vals[2].trim()));
            }
            return reset(lat, lon);
        }
        return resetFromGeoHash(value);
    }

    public GeoPoint resetFromIndexHash(long hash) {
        lon = GeoHashUtils.decodeLongitude(hash);
        lat = GeoHashUtils.decodeLatitude(hash);
        return this;
    }

    public GeoPoint resetFromGeoHash(String geohash) {
        final long hash;
        try {
            hash = mortonEncode(geohash);
        } catch (IllegalArgumentException ex) {
            throw new ElasticsearchParseException(ex.getMessage(), ex);
        }
        return this.reset(GeoHashUtils.decodeLatitude(hash), GeoHashUtils.decodeLongitude(hash));
    }

    public GeoPoint resetFromGeoHash(long geohashLong) {
        final int level = (int) (12 - (geohashLong & 15));
        return this.resetFromIndexHash(BitUtil.flipFlop((geohashLong >>> 4) << ((level * 5) + 2)));
    }

    public double lat() {
        return this.lat;
    }

    public double getLat() {
        return this.lat;
    }

    public double lon() {
        return this.lon;
    }

    public double getLon() {
        return this.lon;
    }

    public String geohash() {
        return stringEncode(lon, lat);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoPoint geoPoint = (GeoPoint) o;

        if (Double.compare(geoPoint.lat, lat) != 0) return false;
        if (Double.compare(geoPoint.lon, lon) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = lat != +0.0d ? Double.doubleToLongBits(lat) : 0L;
        result = Long.hashCode(temp);
        temp = lon != +0.0d ? Double.doubleToLongBits(lon) : 0L;
        result = 31 * result + Long.hashCode(temp);
        return result;
    }

    @Override
    public String toString() {
        return lat + ", " + lon;
    }

    public static GeoPoint fromGeohash(String geohash) {
        return new GeoPoint().resetFromGeoHash(geohash);
    }

    public static GeoPoint fromGeohash(long geohashLong) {
        return new GeoPoint().resetFromGeoHash(geohashLong);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.latlon(lat, lon);
    }

    public static double assertZValue(double zValue) {
        // We removed the `ignore_z_value`, before it defaulted to `true`, so we ignore all z values
        return zValue;
    }
}
