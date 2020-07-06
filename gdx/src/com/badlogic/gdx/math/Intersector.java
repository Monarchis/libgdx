/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.math;

import com.badlogic.gdx.math.Plane.PlaneSide;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

import java.util.Arrays;
import java.util.List;

/** Class offering various static methods for intersection testing between different geometric objects.
 * 
 * @author badlogicgames@gmail.com
 * @author jan.stria
 * @author Nathan Sweet */
public final class Intersector {
	private final static Vector3 v0 = new Vector3();
	private final static Vector3 v1 = new Vector3();
	private final static Vector3 v2 = new Vector3();
	private final static FloatArray floatArray = new FloatArray();
	private final static FloatArray floatArray2 = new FloatArray();
	
	private Intersector () {
	}

	/** Returns whether the given point is inside the triangle. This assumes that the point is on the plane of the triangle. No
	 * check is performed that this is the case.
	 * 
	 * @param point the point
	 * @param t1 the first vertex of the triangle
	 * @param t2 the second vertex of the triangle
	 * @param t3 the third vertex of the triangle
	 * @return whether the point is in the triangle */
	public static boolean isPointInTriangle (Vector3 point, Vector3 t1, Vector3 t2, Vector3 t3) {
		v0.set(t1).sub(point);
		v1.set(t2).sub(point);
		v2.set(t3).sub(point);

		float ab = v0.dot(v1);
		float ac = v0.dot(v2);
		float bc = v1.dot(v2);
		float cc = v2.dot(v2);

		if (bc * ac - cc * ab < 0) return false;
		float bb = v1.dot(v1);
		if (ab * bc - ac * bb < 0) return false;
		return true;
	}

	/** Returns {@code true} if the given point is inside the triangle. */
	public static boolean isPointInTriangle (Vector2 p, Vector2 a, Vector2 b, Vector2 c) {
		float px1 = p.x - a.x;
		float py1 = p.y - a.y;
		boolean side12 = (b.x - a.x) * py1 - (b.y - a.y) * px1 > 0;
		if ((c.x - a.x) * py1 - (c.y - a.y) * px1 > 0 == side12) return false;
		if ((c.x - b.x) * (p.y - b.y) - (c.y - b.y) * (p.x - b.x) > 0 != side12) return false;
		return true;
	}

	/** Returns {@code true} if the given point is inside the triangle. */
	public static boolean isPointInTriangle (float px, float py, float ax, float ay, float bx, float by, float cx, float cy) {
		float px1 = px - ax;
		float py1 = py - ay;
		boolean side12 = (bx - ax) * py1 - (by - ay) * px1 > 0;
		if ((cx - ax) * py1 - (cy - ay) * px1 > 0 == side12) return false;
		if ((cx - bx) * (py - by) - (cy - by) * (px - bx) > 0 != side12) return false;
		return true;
	}

	/** @see #intersectSegmentPlane(Vector3, Vector3, Plane, boolean, Vector3) */
	public static boolean intersectSegmentPlane (Vector3 first, Vector3 second, Plane plane, Vector3 intersection) {
		return intersectSegmentPlane(first, second, plane, false, intersection);
	}

	/** @see #intersectSegmentPlane(float, float, float, float, float, float, float, float, float, float,
	 * boolean, boolean, boolean, Vector3) */
	public static boolean intersectSegmentPlane (Vector3 first, Vector3 second, Plane plane,
												 boolean secondIsDirection, boolean lb, boolean ub,
												 Vector3 intersection) {
		return intersectSegmentPlane(first.x, first.y, first.z, second.x, second.y, second.z, plane, secondIsDirection,
				lb, ub, intersection);
	}

	/** @see #intersectSegmentPlane(float, float, float, float, float, float, float, float, float, float,
	 * boolean, boolean, boolean, Vector3) */
	public static boolean intersectSegmentPlane (Vector3 first, Vector3 second, Plane plane,
												 boolean secondIsDirection,
												 Vector3 intersection) {
		return intersectSegmentPlane(first, second, plane, secondIsDirection, true, true, intersection);
	}

	/** @see #intersectSegmentPlane(float, float, float, float, float, float, float, float, float, float,
	 *  boolean, boolean, boolean, Vector3) */
	public static boolean intersectSegmentPlane (float x1, float y1, float z1, float x2, float y2, float z2, Plane plane,
											   boolean secondIsDirection, boolean lb, boolean ub,
											   Vector3 intersection) {
		return !Float.isInfinite(intersectSegmentPlane(x1, y1, z1, x2, y2, z2,
				plane.getNormal().x, plane.getNormal().y, plane.getNormal().z, plane.getD(), secondIsDirection, lb, ub,
				intersection));
	}

	/** Intersects a segment, defined by two points and a {@link Plane}. The intersection point is stored in intersection
	 * in case an intersection is present. The intersection point can be recovered by {@code first + s * second}, if
	 * the second is used as a direction, else the point is recovered by {@code first + s * (second - first)}.
	 * {@code s} is the return value of this method.
	 *
	 * @param x1 the x-coordinate of the segments's first point
	 * @param y1 the y-coordinate of the segments's first point
	 * @param z1 the z-coordinate of the segments's first point
	 * @param x2 the x-coordinate of the segments's second point
	 * @param y2 the y-coordinate of the segments's second point
	 * @param z2 the z-coordinate of the segments's second point
	 * @param nX the x-direction of the planes's normal
	 * @param nY the Y-direction of the planes's normal
	 * @param nZ the z-direction of the planes's normal
	 * @param dist the distance of the normal
	 * @param lb the lower bound, if disabled, the negative direction is infinite (for lines / negative rays)
	 * @param ub the upper bound, if disabled, the positive direction is infinite (for line / positive rays)
	 * @param secondIsDirection a boolean value that indicates if the second point of the segment is used as a direction
	 *                       to construct the segment
	 * @param intersection the vector, where the intersection point is written to (optional)
	 * @return {@code float} the scalar, {@code Float.POSITIVE_INFINITY} or {@code Float.NEGATIVE_INFINITY} in
	 * 			case of no intersection happens. */
	public static float intersectSegmentPlane (float x1, float y1, float z1, float x2, float y2, float z2,
											   float nX, float nY, float nZ, float dist,
											   boolean secondIsDirection, boolean lb, boolean ub,
											   Vector3 intersection) {
		p.set(nX, nY, nZ, dist);
		Vector3 dir = v0.set(x2, y2, z2);
		Vector3 origin = v1.set(x1, y1, z1);
		if (!secondIsDirection) dir.sub(origin);
		float dot = dir.dot(p.getNormal());
		if (dot != 0) {
			float s = -(origin.dot(p.getNormal()) + p.getD()) / dot;
			if (lb && s < 0) return Float.NEGATIVE_INFINITY;
			if (ub && s > 1) return Float.POSITIVE_INFINITY;
			if (intersection != null) intersection.set(origin).add(v0.set(dir).scl(s));
			return s;
		} else if (p.testPoint(origin) == Plane.PlaneSide.OnPlane) {
			if (intersection != null) intersection.set(origin);
			return 0;
		}
		return Float.POSITIVE_INFINITY;
	}

	/** @see #pointLineSide(float, float, float, float, float, float, float, float, float, boolean) */
	public static Vector3 pointLineSide (Vector3 first, Vector3 second, Vector3 point) {
		return pointLineSide(first, second, point, false);
	}

	/** @see #pointLineSide(float, float, float, float, float, float, float, float, float, boolean) */
	public static Vector3 pointLineSide (Vector3 first, Vector3 second, Vector3 point, boolean secondIsDirection) {
		return pointLineSide(first.x, first.y, first.z, second.x, second.y, second.z, point.x, point.y, point.z, secondIsDirection);
	}

	/** @see #pointLineSide(float, float, float, float, float, float, float, float, float, boolean) */
	public static Vector3 pointLineSide (float firstX, float firstY, float firstZ, float secondX, float secondY, float secondZ,
										 float pointX, float pointY, float pointZ) {
		return pointLineSide(firstX, firstY, firstZ, secondX, secondY, secondZ, pointX, pointY, pointZ, false);
	}

	protected final static Vector3 plsV0 = new Vector3();
	protected final static Vector3 plsV1 = new Vector3();

	/** Determines on which side of the given line the point is.
	 * Left and right are relative to the line's direction which is first to second.
	 *
	 * @param firstX the x-coordinate of the line's first point
	 * @param firstY the y-coordinate of the line's first point
	 * @param firstZ the z-coordinate of the line's first point
	 * @param secondX the x-coordinate of the line's second point
	 * @param secondY the y-coordinate of the line's second point
	 * @param secondZ the z-coordinate of the line's first point
	 * @param pointX the x-coordinate of the point to check
	 * @param pointY the y-coordinate of the point to check
	 * @param pointZ the y-coordinate of the point to check
	 * @param secondIsDirection a boolean value that indicates if the second point of the line is used as a direction
	 *                       to construct the line
	 * @return {@code Vector3} the cross product, each axis is set to
	 * {code 1} the point is on the left of the line in this direction
	 * {code 0} the point is on the line in this direction
	 * {code -1} the point is on the right of the line in this direction */
	public static Vector3 pointLineSide (float firstX, float firstY, float firstZ, float secondX, float secondY, float secondZ,
										 float pointX, float pointY, float pointZ,
										 boolean secondIsDirection) {
		plsV0.set(secondX, secondY, secondZ);
		if (!secondIsDirection) {
			plsV0.sub(firstX, firstY,firstZ);
		}
		plsV1.set(pointX, pointY, pointZ).sub(firstX, firstY, firstZ);
		plsV0.crs(plsV1);
		return plsV0.set(plsV0.x > MathUtils.FLOAT_ROUNDING_ERROR ? 1 : (plsV0.x < -MathUtils.FLOAT_ROUNDING_ERROR ? -1 : 0),
				plsV0.y > MathUtils.FLOAT_ROUNDING_ERROR ? 1 : (plsV0.y < -MathUtils.FLOAT_ROUNDING_ERROR ? -1 : 0),
				plsV0.z > MathUtils.FLOAT_ROUNDING_ERROR ? 1 : (plsV0.z < -MathUtils.FLOAT_ROUNDING_ERROR ? -1 : 0));
	}

	/** @see #pointLineSide(float, float, float, float, float, float, boolean) */
	public static int pointLineSide (Vector2 first, Vector2 second, Vector2 point) {
		return pointLineSide(first, second, point, false);
	}

	/** @see #pointLineSide(float, float, float, float, float, float, boolean) */
	public static int pointLineSide (Vector2 first, Vector2 second, Vector2 point, boolean secondIsDirection) {
		return pointLineSide(first.x, first.y, second.x, second.y, point.x, point.y, secondIsDirection);
	}

	/** @see #pointLineSide(float, float, float, float, float, float, boolean) */
	public static int pointLineSide (float firstX, float firstY, float secondX, float secondY, float pointX, float pointY) {
		return pointLineSide(firstX, firstY, secondX, secondY, pointX, pointY, false);
	}

	/** Determines on which side of the given line the point is.
	 * Left and right are relative to the line's direction which is first to second.
	 *
	 * @param firstX the x-coordinate of the line's first point
	 * @param firstY the y-coordinate of the line's first point
	 * @param secondX the x-coordinate of the line's second point
	 * @param secondY the y-coordinate of the line's second point
	 * @param pointX the x-coordinate of the point to check
	 * @param pointY the y-coordinate of the point to check
	 * @param secondIsDirection a boolean value that indicates if the second point of the line is used as a direction
	 *                       to construct the line
	 * @return {@code 1} if the point is on the left side of the line.
	 * {@code 0} if the point is on the line.
	 * {@code -1} if the point is on the right side of the line. */
	public static int pointLineSide (float firstX, float firstY, float secondX, float secondY, float pointX, float pointY,
									 boolean secondIsDirection) {
		float z;
		if (secondIsDirection) {
			z = secondX * (pointY - firstY) - secondY * (pointX - firstX);
		}
		else {
			z = (secondX - firstX) * (pointY - firstY) - (secondY - firstY) * (pointX - firstX);
		}
		if (z > MathUtils.FLOAT_ROUNDING_ERROR) return 1;
		if (z < -MathUtils.FLOAT_ROUNDING_ERROR) return -1;
		return 0;
	}

	/** Checks whether the given point is in the polygon.
	 * 
	 * @param polygon The polygon vertices passed as an array
	 * @param point The point
	 * @return {@code true} if the point is in the polygon */
	public static boolean isPointInPolygon (Array<Vector2> polygon, Vector2 point) {
		Vector2 last = polygon.peek();
		float x = point.x, y = point.y;
		boolean oddNodes = false;
		for (int i = 0; i < polygon.size; i++) {
			Vector2 vertex = polygon.get(i);
			if ((vertex.y < y && last.y >= y) || (last.y < y && vertex.y >= y)) {
				if (vertex.x + (y - vertex.y) / (last.y - vertex.y) * (last.x - vertex.x) < x) oddNodes = !oddNodes;
			}
			last = vertex;
		}
		return oddNodes;
	}

	/** Checks if the specified point is in the polygon.
	 * 
	 * @param offset Starting polygon index.
	 * @param count Number of array indices to use after offset.
	 * @return {@code true} if the point is in the polygon. */
	public static boolean isPointInPolygon (float[] polygon, int offset, int count, float x, float y) {
		boolean oddNodes = false;
		float sx = polygon[offset], sy = polygon[offset + 1], y1 = sy;
		int yi = offset + 3;
		for (int n = offset + count; yi < n; yi += 2) {
			float y2 = polygon[yi];
			if ((y2 < y && y1 >= y) || (y1 < y && y2 >= y)) {
				float x2 = polygon[yi - 1];
				if (x2 + (y - y2) / (y1 - y2) * (polygon[yi - 3] - x2) < x) oddNodes = !oddNodes;
			}
			y1 = y2;
		}
		if ((sy < y && y1 >= y) || (y1 < y && sy >= y)) {
			if (sx + (y - sy) / (y1 - sy) * (polygon[yi - 3] - sx) < x) oddNodes = !oddNodes;
		}
		return oddNodes;
	}

	private final static Vector2 ip = new Vector2();
	private final static Vector2 ep1 = new Vector2();
	private final static Vector2 ep2 = new Vector2();
	private final static Vector2 s = new Vector2();
	private final static Vector2 e = new Vector2();

	/** Intersects two convex polygons with clockwise vertices and sets the overlap polygon resulting from the intersection.
	 * Follows the Sutherland-Hodgman algorithm.
	 * 
	 * @param p1 The polygon that is being clipped
	 * @param p2 The clip polygon
	 * @param overlap The intersection of the two polygons (can be {@code null}, if an intersection polygon is not needed)
	 * @return Whether the two polygons intersect. */
	public static boolean intersectPolygons (Polygon p1, Polygon p2, Polygon overlap) {
		if (p1.getVertices().length == 0 || p2.getVertices().length == 0) {
			return false;
		}
		Vector2 ip = Intersector.ip, ep1 = Intersector.ep1, ep2 = Intersector.ep2, s = Intersector.s, e = Intersector.e;
		FloatArray floatArray = Intersector.floatArray, floatArray2 = Intersector.floatArray2;
		floatArray.clear();
		floatArray2.clear();
		floatArray2.addAll(p1.getTransformedVertices());
		float[] vertices2 = p2.getTransformedVertices();
		for (int i = 0, last = vertices2.length - 2; i <= last; i += 2) {
			ep1.set(vertices2[i], vertices2[i + 1]);
			// wrap around to beginning of array if index points to end;
			if (i < last)
				ep2.set(vertices2[i + 2], vertices2[i + 3]);
			else
				ep2.set(vertices2[0], vertices2[1]);
			if (floatArray2.size == 0) return false;
			s.set(floatArray2.get(floatArray2.size - 2), floatArray2.get(floatArray2.size - 1));
			for (int j = 0; j < floatArray2.size; j += 2) {
				e.set(floatArray2.get(j), floatArray2.get(j + 1));
				// determine if point is inside clip edge
				boolean side = Intersector.pointLineSide(ep2, ep1, s) > 0;
				if (Intersector.pointLineSide(ep2, ep1, e) > 0) {
					if (!side) {
						Intersector.intersectLines(s, e, ep1, ep2, ip);
						if (floatArray.size < 2 || floatArray.get(floatArray.size - 2) != ip.x
							|| floatArray.get(floatArray.size - 1) != ip.y) {
							floatArray.add(ip.x);
							floatArray.add(ip.y);
						}
					}
					floatArray.add(e.x);
					floatArray.add(e.y);
				} else if (side) {
					Intersector.intersectLines(s, e, ep1, ep2, ip);
					floatArray.add(ip.x);
					floatArray.add(ip.y);
				}
				s.set(e.x, e.y);
			}
			floatArray2.clear();
			floatArray2.addAll(floatArray);
			floatArray.clear();
		}
		if (floatArray2.size != 0) {
			if (overlap != null) {
				if (overlap.getVertices().length == floatArray2.size)
					System.arraycopy(floatArray2.items, 0, overlap.getVertices(), 0, floatArray2.size);
				else
					overlap.setVertices(floatArray2.toArray());
			}
			return true;
		}
		return false;
	}

	/** Returns {@code true} if the specified poygons intersect. */
	static public boolean intersectPolygons (FloatArray polygon1, FloatArray polygon2) {
		if (Intersector.isPointInPolygon(polygon1.items, 0, polygon1.size, polygon2.items[0], polygon2.items[1])) return true;
		if (Intersector.isPointInPolygon(polygon2.items, 0, polygon2.size, polygon1.items[0], polygon1.items[1])) return true;
		return intersectPolygonEdges(polygon1, polygon2);
	}

	/** Returns {@code true} if the lines of the specified poygons intersect. */
	static public boolean intersectPolygonEdges (FloatArray polygon1, FloatArray polygon2) {
		int last1 = polygon1.size - 2, last2 = polygon2.size - 2;
		float[] p1 = polygon1.items, p2 = polygon2.items;
		float x1 = p1[last1], y1 = p1[last1 + 1];
		for (int i = 0; i <= last1; i += 2) {
			float x2 = p1[i], y2 = p1[i + 1];
			float x3 = p2[last2], y3 = p2[last2 + 1];
			for (int j = 0; j <= last2; j += 2) {
				float x4 = p2[j], y4 = p2[j + 1];
				if (intersectSegments(x1, y1, x2, y2, x3, y3, x4, y4, null)) return true;
				x3 = x4;
				y3 = y4;
			}
			x1 = x2;
			y1 = y2;
		}
		return false;
	}

    static Vector2 v2a = new Vector2();
    static Vector2 v2b = new Vector2();
    static Vector2 v2c = new Vector2();
    static Vector2 v2d = new Vector2();

	/** Returns the distance between the given line and point.
	 *
	 * @param startX the x-coordinate of the line's start point
	 * @param startY the y-coordinate of the line's start point
	 * @param endX the x-coordinate of the line's end point
	 * @param endY the y-coordinate of the line's end point
	 * @param pointX the x-coordinate of the point
	 * @param pointY the y-coordinate of the point
	 * @return the distance to the nearest line point */
	public static float distanceLinePoint (float startX, float startY, float endX, float endY, float pointX, float pointY) {
		float normalLength = (float)Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
		return Math.abs((pointX - startX) * (endY - startY) - (pointY - startY) * (endX - startX)) / normalLength;
	}

	/** Returns the distance between the given segment and point.
	 *
	 * @param startX the x-coordinate of the segment's start point
	 * @param startY the y-coordinate of the segment's start point
	 * @param endX the x-coordinate of the segment's end point
	 * @param endY the y-coordinate of the segment's end point
	 * @param pointX the x-coordinate of the point
	 * @param pointY the y-coordinate of the point
	 * @return the distance to the nearest segment point */
	public static float distanceSegmentPoint (float startX, float startY, float endX, float endY, float pointX, float pointY) {
		return nearestSegmentPoint(startX, startY, endX, endY, pointX, pointY, v2a).dst(pointX, pointY);
	}

	/** Returns the distance between the given segment and point.
	 *
	 * @param origin the segment's start point
	 * @param direction the segment's end point
	 * @param point the point
	 * @return the distance to the nearest segment point */
	public static float distanceSegmentPoint (Vector2 origin, Vector2 direction, Vector2 point) {
		return nearestSegmentPoint(origin, direction, point, v2a).dst(point);
	}

	/** @see #nearestSegmentPoint(float, float, float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestSegmentPoint (Vector2 first, Vector2 second, Vector2 point, Vector2 nearest) {
		return nearestSegmentPoint(first, second, point, false, nearest);
	}

	/** @see #nearestSegmentPoint(float, float, float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestSegmentPoint (Vector2 first, Vector2 second, Vector2 point, boolean secondIsDirection, Vector2 nearest) {
		return nearestSegmentPoint(first.x, first.y, second.x, second.y, point.x, point.y, secondIsDirection, nearest);
	}

	/** @see #nearestSegmentPoint(float, float, float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestSegmentPoint (float firstX, float firstY, float secondX, float secondY, float pointX, float pointY,
		Vector2 nearest) {
		return nearestSegmentPoint(firstX, firstY, secondX, secondY, pointX, pointY, false, nearest);
	}

	/** @see #nearestSegmentPoint(float, float, float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestSegmentPoint (float firstX, float firstY, float secondX, float secondY,
											   float pointX, float pointY, boolean secondIsDirection, Vector2 nearest) {
		return nearestSegmentPoint(firstX, firstY, secondX, secondY, 1, 1, pointX, pointY, secondIsDirection, nearest);
	}

	/** Returns a point on the segment nearest to the specified point.
	 *
	 * @param firstX the x-coordinate of the segment's first point
	 * @param firstY the y-coordinate of the segment's first point
	 * @param secondX the x-coordinate of the segment's second point
	 * @param secondY the y-coordinate of the segment's second point
	 * @param lowerScale the scaling of the segment in negative direction
	 * @param upperScale the scaling of the segment in positive direction
	 * @param pointX the x-coordinate of the point
	 * @param pointY the y-coordinate of the point
	 * @param secondIsDirection a boolean value that indicates if the second point of the segment is used as a direction
	 *                       to construct the segment
	 * @param nearest the nearest point (optional)
	 * @return {@code Vector2} the nearest point on the segment relative to the given point. */
	public static Vector2 nearestSegmentPoint (float firstX, float firstY, float secondX, float secondY,
											   float lowerScale, float upperScale, float pointX, float pointY,
											   boolean secondIsDirection, Vector2 nearest) {
		float xDiff = secondX;
		float yDiff = secondY;
		if (!secondIsDirection) {
			xDiff -= firstX;
			yDiff -= firstY;
		}
		float length2 = xDiff * xDiff + yDiff * yDiff;
		if (nearest == null) {
			nearest = v2d;
		}
		if (length2 < MathUtils.FLOAT_ROUNDING_ERROR) return nearest.set(firstX, firstY);
		float t = ((pointX - firstX) * xDiff + (pointY - firstY) * yDiff) / length2;
		if (t > upperScale) return nearest.set(secondX, secondY);
		if (t < 1 - lowerScale) return nearest.set(firstX, firstY);
		return nearest.set(firstX + t * (secondX - firstX), firstY + t * (secondY - firstY));
	}

	/** @see #nearestRayPoint(float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestRayPoint (Vector2 first, Vector2 second, Vector2 point, Vector2 nearest) {
		return nearestRayPoint(first, second, point, false, nearest);
	}

	/** @see #nearestRayPoint(float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestRayPoint (Vector2 first, Vector2 second, Vector2 point, boolean secondIsDirection, Vector2 nearest) {
		return nearestRayPoint(first.x, first.y, second.x, second.y, point.x, point.y, secondIsDirection, nearest);
	}

	/** @see #nearestRayPoint(float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestRayPoint (float firstX, float firstY, float secondX, float secondY, float pointX, float pointY,
											   Vector2 nearest) {
		return nearestRayPoint(firstX, firstY, secondX, secondY, pointX, pointY, false, nearest);
	}

	/** Returns a point on the ray nearest to the specified point.
	 *
	 * @param firstX the x-coordinate of the ray's first point
	 * @param firstY the y-coordinate of the ray's first point
	 * @param secondX the x-coordinate of the ray's second point
	 * @param secondY the y-coordinate of the ray's second point
	 * @param pointX the x-coordinate of the point
	 * @param pointY the y-coordinate of the point
	 * @param secondIsDirection a boolean value that indicates if the second point of the ray is used as a direction
	 *                       to construct the ray
	 * @param nearest the nearest point (optional)
	 * @return {@code Vector2} the nearest point on the ray relative to the given point. */
	public static Vector2 nearestRayPoint (float firstX, float firstY, float secondX, float secondY,
											   float pointX, float pointY, boolean secondIsDirection, Vector2 nearest) {
		return nearestSegmentPoint(firstX, firstY, secondX, secondY, 1, Float.POSITIVE_INFINITY, pointX, pointY,
				secondIsDirection, nearest);
	}

	/** @see #nearestLinePoint(float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestLinePoint (Vector2 first, Vector2 second, Vector2 point, Vector2 nearest) {
		return nearestLinePoint(first, second, point, false, nearest);
	}

	/** @see #nearestLinePoint(float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestLinePoint (Vector2 first, Vector2 second, Vector2 point, boolean secondIsDirection, Vector2 nearest) {
		return nearestLinePoint(first.x, first.y, second.x, second.y, point.x, point.y, secondIsDirection, nearest);
	}

	/** @see #nearestLinePoint(float, float, float, float, float, float, boolean, Vector2) */
	public static Vector2 nearestLinePoint (float firstX, float firstY, float secondX, float secondY, float pointX, float pointY,
										   Vector2 nearest) {
		return nearestLinePoint(firstX, firstY, secondX, secondY, pointX, pointY, false, nearest);
	}

	/** Returns a point on the line nearest to the specified point.
	 *
	 * @param firstX the x-coordinate of the line's first point
	 * @param firstY the y-coordinate of the line's first point
	 * @param secondX the x-coordinate of the line's second point
	 * @param secondY the y-coordinate of the line's second point
	 * @param pointX the x-coordinate of the point
	 * @param pointY the y-coordinate of the point
	 * @param secondIsDirection a boolean value that indicates if the second point of the line is used as a direction
	 *                       to construct the line
	 * @param nearest the nearest point (optional)
	 * @return {@code Vector2} the nearest point on the line relative to the given point. */
	public static Vector2 nearestLinePoint (float firstX, float firstY, float secondX, float secondY,
										   float pointX, float pointY, boolean secondIsDirection, Vector2 nearest) {
		return nearestSegmentPoint(firstX, firstY, secondX, secondY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
				pointX, pointY, secondIsDirection, nearest);
	}

	/** Checks whether the given line segment intersects the given circle.
	 * 
	 * @param start The start point of the line segment
	 * @param end The end point of the line segment
	 * @param center The center of the circle
	 * @param squareRadius The squared radius of the circle
	 * @return Whether the line segment and the circle intersect */
	public static boolean intersectSegmentCircle (Vector2 start, Vector2 end, Vector2 center, float squareRadius) {
		tmp.set(end.x - start.x, end.y - start.y, 0);
		tmp1.set(center.x - start.x, center.y - start.y, 0);
		float l = tmp.len();
		float u = tmp1.dot(tmp.nor());
		if (u <= 0) {
			tmp2.set(start.x, start.y, 0);
		} else if (u >= l) {
			tmp2.set(end.x, end.y, 0);
		} else {
			tmp3.set(tmp.scl(u)); // remember tmp is already normalized
			tmp2.set(tmp3.x + start.x, tmp3.y + start.y, 0);
		}

		float x = center.x - tmp2.x;
		float y = center.y - tmp2.y;

		return x * x + y * y <= squareRadius;
	}

	/** Checks whether the given line segment intersects the given circle.
	 * 
	 * @param start The start point of the line segment
	 * @param end The end point of the line segment
	 * @param circle The circle
	 * @param mtv A Minimum Translation Vector to fill in the case of a collision, or {@code null} (optional).
	 * @return Whether the line segment and the circle intersect */
	public static boolean intersectSegmentCircle (Vector2 start, Vector2 end, Circle circle, MinimumTranslationVector mtv) {
		v2a.set(end).sub(start);
		v2b.set(circle.x - start.x, circle.y - start.y);
		float len = v2a.len();
		float u = v2b.dot(v2a.nor());
		if (u <= 0) {
			v2c.set(start);
		} else if (u >= len) {
			v2c.set(end);
		} else {
			v2d.set(v2a.scl(u)); // remember v2a is already normalized
			v2c.set(v2d).add(start);
		}

		v2a.set(v2c.x - circle.x, v2c.y - circle.y);

		if (mtv != null) {
			// Handle special case of segment containing circle center
			if (v2a.equals(Vector2.Zero)) {
				v2d.set(end.y - start.y, start.x - end.x);
				mtv.normal.set(v2d).nor();
				mtv.depth = circle.radius;
			} else {
				mtv.normal.set(v2a).nor();
				mtv.depth = circle.radius - v2a.len();
			}
		}

		return v2a.len2() <= circle.radius * circle.radius;
	}

	/** @see #intersectRayRay(float, float, float, float, float, float, float, float, boolean, Vector2) */
	public static float intersectRayRay (Vector2 origin1, Vector2 direction1, Vector2 origin2, Vector2 direction2) {
		return intersectRayRay(origin1, direction1, origin2, direction2,
				true, null);
	}

	/** @see #intersectRayRay(float, float, float, float, float, float, float, float, boolean, Vector2) */
	public static float intersectRayRay (Vector2 first1, Vector2 second1, Vector2 first2, Vector2 second2,
										 boolean secondIsDirection, Vector2 intersection) {
		return intersectRayRay(first1.x, first1.y, second1.x, second1.y, first2.x, first2.y, second2.x, second2.y,
				secondIsDirection, intersection);
	}

	/** Intersects a ray, defined by 2 point and another ray, also defined by two points.
	 * The intersection point is stored in intersection in case an intersection is present. The intersection point can
	 * be recovered by {@code first1 + s * (second1 - first1)} if secondIsDirection is false. if true,
	 * the intersection point is recovered by {@code first1 + s * second1}. {@code s} is the return value of
	 * this method.
	 *
	 * @param first1X the x-coordinate of the ray's first point
	 * @param first1Y the y-coordinate of the ray's first point
	 * @param second1X the x-coordinate of the ray's second point / direction
	 * @param second1Y the y-coordinate of the ray's second point / direction
	 * @param secondIsDirection a boolean value that indicates if the direction of the ray is used as a direction
	 *                       to construct the ray or if it's used as a point.
	 * @param intersection the vector, where the intersection point is written to (optional)
	 * @return {@code float} the scalar, {@code Float.POSITIVE_INFINITY} or {@code Float.NEGATIVE_INFINITY} in
	 * 			case of no intersection happens. */
	public static float intersectRayRay (float first1X, float first1Y, float second1X, float second1Y,
										   float first2X, float first2Y, float second2X, float second2Y,
										   boolean secondIsDirection, Vector2 intersection) {
		return intersectSegments(first1X, first1Y, second1X, second1Y,
				first2X, first2Y, second2X, second2Y, secondIsDirection, true, false, intersection);
	}

	/** @see #intersectRayPlane(float, float, float, float, float, float, float, float, float, float, boolean, Vector3) */
	public static boolean intersectRayPlane (Ray ray, Plane plane, Vector3 intersection) {
		return intersectRayPlane(ray, plane, true, intersection);
	}

	/** @see #intersectRayPlane(float, float, float, float, float, float,
	 * float, float, float, float, boolean, Vector3) */
	public static boolean intersectRayPlane (Ray ray, Plane plane, boolean secondIsDirection, Vector3 intersection) {
		return intersectRayPlane(ray.origin, ray.direction, plane, secondIsDirection, intersection);
	}

	/** @see #intersectRayPlane(Vector3, Vector3, Plane, boolean, Vector3) */
	public static boolean intersectRayPlane (Vector3 first, Vector3 second, Plane plane, Vector3 intersection) {
		return intersectRayPlane(first, second, plane, false, intersection);
	}

	/** @see #intersectRayPlane(float, float, float, float, float, float,
	 *	float, float, float, float, boolean, Vector3) */
	public static boolean intersectRayPlane (Vector3 first, Vector3 second, Plane plane, boolean secondIsDirection,
	 	Vector3 intersection) {
		return intersectRayPlane(first.x, first.y, first.z, second.x, second.y, second.z, plane, secondIsDirection,
				intersection);
	}

	/** @see #intersectRayPlane(float, float, float, float, float, float, float, float, float, float, boolean, Vector3) */
	public static boolean intersectRayPlane (float x1, float y1, float z1, float x2, float y2, float z2, Plane plane,
										   boolean secondIsDirection, Vector3 intersection) {
		return !Float.isInfinite(intersectSegmentPlane(x1, y1, z1, x2, y2, z2,
				plane.getNormal().x, plane.getNormal().y, plane.getNormal().z, plane.getD(),
				secondIsDirection, true, false, intersection));
	}

	/** Intersects a ray, defined by 2 point and a {@link Plane}, defined by it's normal and direction.
	 * The intersection point is stored in intersection in case an intersection is present. The intersection point can
	 * be recovered by {@code ray.origin + s * (ray.direction - ray.origin)} if secondIsDirection is false. if true,
	 * the intersection point is recovered by {@code ray.origin + s * ray.direction}. {@code s} is the return value of
	 * this method.
	 *
	 * @param x1 the x-coordinate of the ray's first point
	 * @param y1 the y-coordinate of the ray's first point
	 * @param z1 the z-coordinate of the ray's first point
	 * @param x2 the x-coordinate of the ray's second point / direction
	 * @param y2 the y-coordinate of the ray's second point / direction
	 * @param z2 the z-coordinate of the ray's second point / direction
	 * @param secondIsDirection a boolean value that indicates if the direction of the ray is used as a direction
	 *                       to construct the ray or if it's used as a point.
	 * @param intersection the vector, where the intersection point is written to (optional)
	 * @return {@code float} the scalar, {@code Float.POSITIVE_INFINITY} or {@code Float.NEGATIVE_INFINITY} in
	 * 			case of no intersection happens. */
	public static float intersectRayPlane (float x1, float y1, float z1, float x2, float y2, float z2,
											   float nX, float nY, float nZ, float dist,
											   boolean secondIsDirection, Vector3 intersection) {
		return intersectSegmentPlane(x1, y1, z1, x2, y2, z2, nX, nY, nZ, dist, secondIsDirection, true, false, intersection);
	}

	/** @see #intersectLinePlane(float, float, float, float, float, float, float, float, float, float, boolean, Vector3) */
	public static boolean intersectLinePlane (Ray ray, Plane plane, Vector3 intersection) {
		return intersectLinePlane(ray, plane, true, intersection);
	}

	/** @see #intersectLinePlane(float, float, float, float, float, float, float, float, float, float, boolean, Vector3) */
	public static boolean intersectLinePlane (Ray ray, Plane plane, boolean secondIsDirection, Vector3 intersection) {
		return intersectLinePlane(ray.origin, ray.direction, plane, secondIsDirection, intersection);
	}

	/** @see #intersectLinePlane(Vector3, Vector3, Plane, boolean, Vector3) */
	public static boolean intersectLinePlane (Vector3 first, Vector3 second, Plane plane, Vector3 intersection) {
		return intersectLinePlane(first, second, plane, false, intersection);
	}

	/** @see #intersectLinePlane(float, float, float, float, float, float, float, float, float, float, boolean, Vector3) */
	public static boolean intersectLinePlane (Vector3 first, Vector3 second, Plane plane, boolean secondIsDirection,
											 Vector3 intersection) {
		return !Float.isInfinite(intersectLinePlane(first.x, first.y, first.z, second.x, second.y, second.z,
				plane.getNormal().x, plane.getNormal().y, plane.getNormal().z, plane.getD(),
				secondIsDirection, intersection));
	}

	/** @see #intersectLinePlane(float, float, float, float, float, float, float, float, float, float, boolean, Vector3) */
	public static float intersectLinePlane (float x, float y, float z, float x2, float y2, float z2, Plane plane,
		Vector3 intersection) {
		return intersectLinePlane(x, y, z, x2, y2, z2, plane.normal.x, plane.normal.y,plane.normal.z, plane.d, false, intersection);
	}

	/** Intersects a line, defined by 2 point and a {@link Plane}, defined by it's normal and direction.
	 * The intersection point is stored in intersection in case an intersection is present. The intersection point can
	 * be recovered by {@code point1 + s * (point2 - point1)} if secondIsDirection is false. if true,
	 * the intersection point is recovered by {@code point1 + s * point1}. {@code s} is the return value of
	 * this method.
	 *
	 * @param x1 the x-coordinate of the lines's first point
	 * @param y1 the y-coordinate of the lines's first point
	 * @param z1 the z-coordinate of the lines's first point
	 * @param x2 the x-coordinate of the lines's second point / direction
	 * @param y2 the y-coordinate of the lines's second point / direction
	 * @param z2 the z-coordinate of the lines's second point / direction
	 * @param secondIsDirection a boolean value that indicates if the direction of the line is used as a direction
	 *                       to construct the line or if it's used as a point.
	 * @param intersection the vector, where the intersection point is written to (optional)
	 * @return {@code float} the scalar, {@code Float.POSITIVE_INFINITY} or {@code Float.NEGATIVE_INFINITY} in
	 * 			case of no intersection happens. */
	public static float intersectLinePlane (float x1, float y1, float z1, float x2, float y2, float z2,
										   float nX, float nY, float nZ, float dist,
										   boolean secondIsDirection, Vector3 intersection) {
		return intersectSegmentPlane(x1, y1, z1, x2, y2, z2, nX, nY, nZ, dist, secondIsDirection, false, false, intersection);
	}

	private static final Plane p = new Plane(new Vector3(), 0);
	private static final Vector3 i = new Vector3();

	/** Intersect a {@link Ray} and a triangle, returning the intersection point in intersection.
	 * 
	 * @param ray The ray
	 * @param t1 The first vertex of the triangle
	 * @param t2 The second vertex of the triangle
	 * @param t3 The third vertex of the triangle
	 * @param intersection The intersection point (optional)
	 * @return {@code true} in case an intersection is present. */
	public static boolean intersectRayTriangle (Ray ray, Vector3 t1, Vector3 t2, Vector3 t3, Vector3 intersection) {
		Vector3 edge1 = v0.set(t2).sub(t1);
		Vector3 edge2 = v1.set(t3).sub(t1);

		Vector3 pvec = v2.set(ray.direction).crs(edge2);
		float det = edge1.dot(pvec);
		if (MathUtils.isZero(det)) {
			p.set(t1, t2, t3);
			if (p.testPoint(ray.origin) == PlaneSide.OnPlane && Intersector.isPointInTriangle(ray.origin, t1, t2, t3)) {
				if (intersection != null) intersection.set(ray.origin);
				return true;
			}
			return false;
		}

		det = 1.0f / det;

		Vector3 tvec = i.set(ray.origin).sub(t1);
		float u = tvec.dot(pvec) * det;
		if (u < 0.0f || u > 1.0f) return false;

		Vector3 qvec = tvec.crs(edge1);
		float v = ray.direction.dot(qvec) * det;
		if (v < 0.0f || u + v > 1.0f) return false;

		float t = edge2.dot(qvec) * det;
		if (t < 0) return false;

		if (intersection != null) {
			if (t <= MathUtils.FLOAT_ROUNDING_ERROR) {
				intersection.set(ray.origin);
			} else {
				ray.getEndPoint(intersection, t);
			}
		}

		return true;
	}

	private static final Vector3 dir = new Vector3();
	private static final Vector3 start = new Vector3();

	/** Intersects a {@link Ray} and a sphere, returning the intersection point in intersection.
	 * 
	 * @param ray The ray, the direction component must be normalized before calling this method
	 * @param center The center of the sphere
	 * @param radius The radius of the sphere
	 * @param intersection The intersection point (optional, can be {@code null})
	 * @return Whether an intersection is present. */
	public static boolean intersectRaySphere (Ray ray, Vector3 center, float radius, Vector3 intersection) {
		final float len = ray.direction.dot(center.x - ray.origin.x, center.y - ray.origin.y, center.z - ray.origin.z);
		if (len < 0.f) // behind the ray
			return false;
		final float dst2 = center.dst2(ray.origin.x + ray.direction.x * len, ray.origin.y + ray.direction.y * len,
			ray.origin.z + ray.direction.z * len);
		final float r2 = radius * radius;
		if (dst2 > r2) return false;
		if (intersection != null) intersection.set(ray.direction).scl(len - (float)Math.sqrt(r2 - dst2)).add(ray.origin);
		return true;
	}

	/** Intersects a {@link Ray} and a {@link BoundingBox}, returning the intersection point in intersection. This intersection is
	 * defined as the point on the ray closest to the origin which is within the specified bounds.
	 * 
	 * <p>
	 * The returned intersection (if any) is guaranteed to be within the bounds of the bounding box, but it can occasionally
	 * diverge slightly from ray, due to small floating-point errors.
	 * </p>
	 * 
	 * <p>
	 * If the origin of the ray is inside the box, this method returns true and the intersection point is set to the origin of the
	 * ray, accordingly to the definition above.
	 * </p>
	 * 
	 * @param ray The ray
	 * @param box The box
	 * @param intersection The intersection point (optional)
	 * @return Whether an intersection is present. */
	public static boolean intersectRayBounds (Ray ray, BoundingBox box, Vector3 intersection) {
		if (box.contains(ray.origin)) {
			if (intersection != null) intersection.set(ray.origin);
			return true;
		}
		float lowest = 0, t;
		boolean hit = false;

		// min x
		if (ray.origin.x <= box.min.x && ray.direction.x > 0) {
			t = (box.min.x - ray.origin.x) / ray.direction.x;
			if (t >= 0) {
				v2.set(ray.direction).scl(t).add(ray.origin);
				if (v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
					hit = true;
					lowest = t;
				}
			}
		}
		// max x
		if (ray.origin.x >= box.max.x && ray.direction.x < 0) {
			t = (box.max.x - ray.origin.x) / ray.direction.x;
			if (t >= 0) {
				v2.set(ray.direction).scl(t).add(ray.origin);
				if (v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
					hit = true;
					lowest = t;
				}
			}
		}
		// min y
		if (ray.origin.y <= box.min.y && ray.direction.y > 0) {
			t = (box.min.y - ray.origin.y) / ray.direction.y;
			if (t >= 0) {
				v2.set(ray.direction).scl(t).add(ray.origin);
				if (v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
					hit = true;
					lowest = t;
				}
			}
		}
		// max y
		if (ray.origin.y >= box.max.y && ray.direction.y < 0) {
			t = (box.max.y - ray.origin.y) / ray.direction.y;
			if (t >= 0) {
				v2.set(ray.direction).scl(t).add(ray.origin);
				if (v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
					hit = true;
					lowest = t;
				}
			}
		}
		// min z
		if (ray.origin.z <= box.min.z && ray.direction.z > 0) {
			t = (box.min.z - ray.origin.z) / ray.direction.z;
			if (t >= 0) {
				v2.set(ray.direction).scl(t).add(ray.origin);
				if (v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)) {
					hit = true;
					lowest = t;
				}
			}
		}
		// max y
		if (ray.origin.z >= box.max.z && ray.direction.z < 0) {
			t = (box.max.z - ray.origin.z) / ray.direction.z;
			if (t >= 0) {
				v2.set(ray.direction).scl(t).add(ray.origin);
				if (v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)) {
					hit = true;
					lowest = t;
				}
			}
		}
		if (hit && intersection != null) {
			intersection.set(ray.direction).scl(lowest).add(ray.origin);
			if (intersection.x < box.min.x) {
				intersection.x = box.min.x;
			} else if (intersection.x > box.max.x) {
				intersection.x = box.max.x;
			}
			if (intersection.y < box.min.y) {
				intersection.y = box.min.y;
			} else if (intersection.y > box.max.y) {
				intersection.y = box.max.y;
			}
			if (intersection.z < box.min.z) {
				intersection.z = box.min.z;
			} else if (intersection.z > box.max.z) {
				intersection.z = box.max.z;
			}
		}
		return hit;
	}

	/** Quick check whether the given {@link Ray} and {@link BoundingBox} intersect.
	 * 
	 * @param ray The ray
	 * @param box The bounding box
	 * @return Whether the ray and the bounding box intersect. */
	static public boolean intersectRayBoundsFast (Ray ray, BoundingBox box) {
		return intersectRayBoundsFast(ray, box.getCenter(tmp1), box.getDimensions(tmp2));
	}

	/** Quick check whether the given {@link Ray} and {@link BoundingBox} intersect.
	 * 
	 * @param ray The ray
	 * @param center The center of the bounding box
	 * @param dimensions The dimensions (width, height and depth) of the bounding box
	 * @return Whether the ray and the bounding box intersect. */
	static public boolean intersectRayBoundsFast (Ray ray, Vector3 center, Vector3 dimensions) {
		final float divX = 1f / ray.direction.x;
		final float divY = 1f / ray.direction.y;
		final float divZ = 1f / ray.direction.z;

		float minx = ((center.x - dimensions.x * .5f) - ray.origin.x) * divX;
		float maxx = ((center.x + dimensions.x * .5f) - ray.origin.x) * divX;
		if (minx > maxx) {
			final float t = minx;
			minx = maxx;
			maxx = t;
		}

		float miny = ((center.y - dimensions.y * .5f) - ray.origin.y) * divY;
		float maxy = ((center.y + dimensions.y * .5f) - ray.origin.y) * divY;
		if (miny > maxy) {
			final float t = miny;
			miny = maxy;
			maxy = t;
		}

		float minz = ((center.z - dimensions.z * .5f) - ray.origin.z) * divZ;
		float maxz = ((center.z + dimensions.z * .5f) - ray.origin.z) * divZ;
		if (minz > maxz) {
			final float t = minz;
			minz = maxz;
			maxz = t;
		}

		float min = Math.max(Math.max(minx, miny), minz);
		float max = Math.min(Math.min(maxx, maxy), maxz);

		return max >= 0 && max >= min;
	}

	static Vector3 best = new Vector3();
	static Vector3 tmp = new Vector3();
	static Vector3 tmp1 = new Vector3();
	static Vector3 tmp2 = new Vector3();
	static Vector3 tmp3 = new Vector3();

	/** Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
	 * 
	 * @param ray The ray
	 * @param triangles The triangles, each successive 9 elements are the 3 vertices of a triangle, a vertex is made of 3
	 *           successive floats (XYZ)
	 * @param intersection The nearest intersection point (optional)
	 * @return Whether the ray and the triangles intersect. */
	public static boolean intersectRayTriangles (Ray ray, float[] triangles, Vector3 intersection) {
		float min_dist = Float.MAX_VALUE;
		boolean hit = false;

		if (triangles.length % 9 != 0) throw new RuntimeException("triangles array size is not a multiple of 9");

		for (int i = 0; i < triangles.length; i += 9) {
			boolean result = intersectRayTriangle(ray, tmp1.set(triangles[i], triangles[i + 1], triangles[i + 2]),
				tmp2.set(triangles[i + 3], triangles[i + 4], triangles[i + 5]),
				tmp3.set(triangles[i + 6], triangles[i + 7], triangles[i + 8]), tmp);

			if (result) {
				float dist = ray.origin.dst2(tmp);
				if (dist < min_dist) {
					min_dist = dist;
					best.set(tmp);
					hit = true;
				}
			}
		}

		if (!hit)
			return false;
		else {
			if (intersection != null) intersection.set(best);
			return true;
		}
	}

	/** Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
	 * 
	 * @param ray The ray
	 * @param vertices the vertices
	 * @param indices the indices, each successive 3 shorts index the 3 vertices of a triangle
	 * @param vertexSize the size of a vertex in floats
	 * @param intersection The nearest intersection point (optional)
	 * @return Whether the ray and the triangles intersect. */
	public static boolean intersectRayTriangles (Ray ray, float[] vertices, short[] indices, int vertexSize,
		Vector3 intersection) {
		float min_dist = Float.MAX_VALUE;
		boolean hit = false;

		if (indices.length % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3");

		for (int i = 0; i < indices.length; i += 3) {
			int i1 = indices[i] * vertexSize;
			int i2 = indices[i + 1] * vertexSize;
			int i3 = indices[i + 2] * vertexSize;

			boolean result = intersectRayTriangle(ray, tmp1.set(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]),
				tmp2.set(vertices[i2], vertices[i2 + 1], vertices[i2 + 2]),
				tmp3.set(vertices[i3], vertices[i3 + 1], vertices[i3 + 2]), tmp);

			if (result) {
				float dist = ray.origin.dst2(tmp);
				if (dist < min_dist) {
					min_dist = dist;
					best.set(tmp);
					hit = true;
				}
			}
		}

		if (!hit)
			return false;
		else {
			if (intersection != null) intersection.set(best);
			return true;
		}
	}

	/** Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
	 * 
	 * @param ray The ray
	 * @param triangles The triangles, each successive 3 elements are the 3 vertices of a triangle
	 * @param intersection The nearest intersection point (optional)
	 * @return Whether the ray and the triangles intersect. */
	public static boolean intersectRayTriangles (Ray ray, List<Vector3> triangles, Vector3 intersection) {
		float min_dist = Float.MAX_VALUE;
		boolean hit = false;

		if (triangles.size() % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3");

		for (int i = 0; i < triangles.size(); i += 3) {
			boolean result = intersectRayTriangle(ray, triangles.get(i), triangles.get(i + 1), triangles.get(i + 2), tmp);

			if (result) {
				float dist = ray.origin.dst2(tmp);
				if (dist < min_dist) {
					min_dist = dist;
					best.set(tmp);
					hit = true;
				}
			}
		}

		if (!hit)
			return false;
		else {
			if (intersection != null) intersection.set(best);
			return true;
		}
	}

	/**
	 * Quick check whether the given {@link BoundingBox} and {@link Plane} intersect.
	 *
	 * @param box The bounding box
	 * @param plane The plane
	 * @return Whether the bounding box and the plane intersect. */
	public static boolean intersectBoundsPlaneFast (BoundingBox box, Plane plane) {
		return intersectBoundsPlaneFast(box.getCenter(tmp1), box.getDimensions(tmp2).scl(0.5f), plane.normal, plane.d);
	}

	/**
	 * Quick check whether the given bounding box and a plane intersect.
	 * Code adapted from Christer Ericson's Real Time Collision
	 *
	 * @param center The center of the bounding box
	 * @param halfDimensions Half of the dimensions (width, height and depth) of the bounding box
	 * @param normal The normal of the plane
	 * @param distance The distance of the plane
	 * @return Whether the bounding box and the plane intersect. */
	public static boolean intersectBoundsPlaneFast (Vector3 center, Vector3 halfDimensions, Vector3 normal, float distance) {
		// Compute the projection interval radius of b onto L(t) = b.c + t * p.n
		float radius = halfDimensions.x * Math.abs(normal.x) +
			halfDimensions.y * Math.abs(normal.y) +
			halfDimensions.z * Math.abs(normal.z);

		// Compute distance of box center from plane
		float s = normal.dot(center) - distance;

		// Intersection occurs when plane distance falls within [-r,+r] interval
		return Math.abs(s) <= radius;
	}

	/** @see #intersectLines(float, float, float, float, float, float, float, float, boolean, Vector2) */
	public static boolean intersectLines (Vector2 first1, Vector2 second1, Vector2 first2, Vector2 second2, Vector2 intersection) {
		return intersectLines(first1, second1, first2, second2, false, intersection);
	}

	/** @see #intersectLines(float, float, float, float, float, float, float, float, boolean, Vector2) */
	public static boolean intersectLines (Vector2 first1, Vector2 second1, Vector2 first2, Vector2 second2,
										  boolean secondIsDirection, Vector2 intersection) {
		return !Float.isInfinite(intersectLines(first1.x, first1.y, second1.x, second1.y, first2.x, first2.y, second2.x, second2.y,
				secondIsDirection, intersection));
	}

	/** @see #intersectLines(float, float, float, float, float, float, float, float, boolean, Vector2) */
	public static boolean intersectLines (float first1X, float first1Y, float second1X, float second1Y,
										  float first2X, float first2Y, float second2X, float second2Y,
										  Vector2 intersection) {
		return !Float.isInfinite(intersectLines(first1X, first1Y, second1X, second1Y, first2X, first2Y, second2X, second2Y,
				false, intersection));
	}

	/** Returns true if the two lines intersect and returns the intersection point in {@code Vector2 intersection}.
	 *
	 * @param first1X The first point's x-coordinate of the first line
	 * @param first1Y The first point's y-coordinate of the first line
	 * @param second1X The second point's x-coordinate of the first line
	 * @param second1Y The second point's y-coordinate of the first line
	 * @param first2X The first point's x-coordinate of the second line
	 * @param first2Y The first point's y-coordinate of the second line
	 * @param second2X The second point's x-coordinate of the second line
	 * @param second2Y The second point's y-coordinate of the second line
	 * @param secondIsDirection a boolean value that indicates if the second point of the line is used as a direction
	 *                       to construct the line
	 * @param intersection The intersection point (optional).
	 * @return {@code float} whether the two lines intersect, and returns the scalar to restore the intersection */
	public static float intersectLines (float first1X, float first1Y, float second1X, float second1Y,
										float first2X, float first2Y, float second2X, float second2Y,
										boolean secondIsDirection, Vector2 intersection) {
		return intersectSegments(first1X, first1Y, second1X, second1Y, first2X, first2Y, second2X, second2Y,
				secondIsDirection, false, false, intersection);
	}

	/** Check whether the given line and {@link Polygon} intersect.
	 * 
	 * @param p1 The first point of the line
	 * @param p2 The second point of the line
	 * @param polygon The polygon
	 * @return Whether polygon and line intersects */
	public static boolean intersectLinePolygon (Vector2 p1, Vector2 p2, Polygon polygon) {
		float[] vertices = polygon.getTransformedVertices();
		float x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y;
		int n = vertices.length;
		float x3 = vertices[n - 2], y3 = vertices[n - 1];
		for (int i = 0; i < n; i += 2) {
			float x4 = vertices[i], y4 = vertices[i + 1];
			float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
			if (d != 0) {
				float yd = y1 - y3;
				float xd = x1 - x3;
				float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
				if (ua >= 0 && ua <= 1) {
					return true;
				}
			}
			x3 = x4;
			y3 = y4;
		}
		return false;
	}

	/** Determines whether the given rectangles intersect and if they do, sets the supplied intersection rectangle to the
	 * area of overlap.
	 *
	 * @param rectangle1 the first rectangle
	 * @param rectangle1 the second rectangle
	 * @param intersection the intersection (optional)
	 * @return {@code true} whether the rectangles intersect */
	public static boolean intersectRectangles (Rectangle rectangle1, Rectangle rectangle2, Rectangle intersection) {
		if (rectangle1.overlaps(rectangle2)) {
			if (intersection != null) {
				intersection.x = Math.max(rectangle1.x, rectangle2.x);
				intersection.width = Math.min(rectangle1.x + rectangle1.width, rectangle2.x + rectangle2.width) - intersection.x;
				intersection.y = Math.max(rectangle1.y, rectangle2.y);
				intersection.height = Math.min(rectangle1.y + rectangle1.height, rectangle2.y + rectangle2.height) - intersection.y;
			}
			return true;
		}
		return false;
	}

	/** Determines whether the given rectangle and segment intersect
	 * 
	 * @param startX x-coordinate start of line segment
	 * @param startY y-coordinate start of line segment
	 * @param endX y-coordinate end of line segment
	 * @param endY y-coordinate end of line segment
	 * @param rectangle rectangle that is being tested for collision
	 * @return {@code true} whether the rectangle intersects with the line segment */
	public static boolean intersectSegmentRectangle (float startX, float startY, float endX, float endY, Rectangle rectangle) {
		float rectangleEndX = rectangle.x + rectangle.width;
		float rectangleEndY = rectangle.y + rectangle.height;

		if (rectangle.contains(startX, startY) || rectangle.contains(endX, endY)) return true;

		if (intersectSegments(startX, startY, endX, endY, rectangle.x, rectangle.y, rectangle.x, rectangleEndY, null))
			return true;

		if (intersectSegments(startX, startY, endX, endY, rectangle.x, rectangle.y, rectangleEndX, rectangle.y, null))
			return true;

		if (intersectSegments(startX, startY, endX, endY, rectangleEndX, rectangle.y, rectangleEndX, rectangleEndY, null))
			return true;

		if (intersectSegments(startX, startY, endX, endY, rectangle.x, rectangleEndY, rectangleEndX, rectangleEndY, null))
			return true;

		return false;
	}

	/** {@link #intersectSegmentRectangle(float, float, float, float, Rectangle)} */
	public static boolean intersectSegmentRectangle (Vector2 start, Vector2 end, Rectangle rectangle) {
		return intersectSegmentRectangle(start.x, start.y, end.x, end.y, rectangle);
	}

	/** Check whether the given line segment and {@link Polygon} intersect.
	 * 
	 * @param p1 The first point of the segment
	 * @param p2 The second point of the segment
	 * @return Whether polygon and segment intersect */
	public static boolean intersectSegmentPolygon (Vector2 p1, Vector2 p2, Polygon polygon) {
		float[] vertices = polygon.getTransformedVertices();
		float x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y;
		int n = vertices.length;
		float x3 = vertices[n - 2], y3 = vertices[n - 1];
		for (int i = 0; i < n; i += 2) {
			float x4 = vertices[i], y4 = vertices[i + 1];
			float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
			if (d != 0) {
				float yd = y1 - y3;
				float xd = x1 - x3;
				float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
				if (ua >= 0 && ua <= 1) {
					float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
					if (ub >= 0 && ub <= 1) {
						return true;
					}
				}
			}
			x3 = x4;
			y3 = y4;
		}
		return false;
	}

	/** @see #intersectSegments(float, float, float, float, float, float, float, float, boolean, boolean, boolean, Vector2) */
	public static boolean intersectSegments (Vector2 first1, Vector2 second1, Vector2 first2, Vector2 second2,
		 Vector2 intersection) {
		return intersectSegments(first1.x, first1.y, second1.x, second1.y, first2.x, first2.y, second2.x, second2.y, intersection);
	}

	/** @see #intersectSegments(float, float, float, float, float, float, float, float, boolean, boolean, boolean, Vector2) */
	public static boolean intersectSegments (Vector2 first1, Vector2 second1, Vector2 first2, Vector2 second2,
											 boolean secondIsDirection, Vector2 intersection) {
		return intersectSegments(first1.x, first1.y,second1.x, second1.y, first2.x, first2.y, second2.x, second2.y,
				secondIsDirection, intersection);
	}

	/** @see #intersectSegments(float, float, float, float, float, float, float, float, boolean, boolean, boolean, Vector2) */
	public static boolean intersectSegments (float first1X, float first1Y, float second1X, float second1Y,
											 float first2X, float first2Y, float second2X, float second2Y,
											 Vector2 intersection) {
		return intersectSegments(first1X, first1Y, second1X, second1Y, first2X, first2Y, second2X, second2Y, false, intersection);
	}

	/** @see #intersectSegments(float, float, float, float, float, float, float, float, boolean, boolean, boolean, Vector2) */
	public static boolean intersectSegments (float first1X, float first1Y, float second1X, float second1Y,
											 float first2X, float first2Y, float second2X, float second2Y,
											 boolean secondIsDirection, Vector2 intersection) {
		return !Float.isInfinite(intersectSegments(first1X, first1Y, second1X, second1Y,
				first2X, first2Y, second2X, second2Y, secondIsDirection, true, true, intersection));
	}

	/** Determines whether the two segments intersect and returns the intersection point in intersection.
	 *
	 * @param first1X The x-coordinate of the first segments's first point
	 * @param first1Y The y-coordinate of the first segments's first point
	 * @param second1X The x-coordinate of the first segments's second point
	 * @param second1Y The y-coordinate of the first segments's second point
	 * @param first2X The x-coordinate of the second segments's first point
	 * @param first2Y The y-coordinate of the second segments's first point
	 * @param second2X The x-coordinate of the second segments's second point
	 * @param second2Y The y-coordinate of the second segments's second point
	 * @param lb the lower bound, if disabled, the negative direction is infinite (for lines / negative rays)
	 * @param ub the upper bound, if disabled, the positive direction is infinite (for line / positive rays)
	 * @param secondIsDirection a boolean value that indicates if the second point of the segment is used as a direction
	 *                       to construct the segment
	 * @param intersection The intersection point (optional).
	 * @return {@code float} whether the two segments intersect, and returns the scalar to restore the intersection */
	public static float intersectSegments (float first1X, float first1Y, float second1X, float second1Y,
											 float first2X, float first2Y, float second2X, float second2Y,
											 boolean secondIsDirection, boolean lb, boolean ub,
											 Vector2 intersection) {
		float xDir1 = second1X;
		float yDir1 = second1Y;
		float xDir2 = second2X;
		float yDir2 = second2Y;
		if (!secondIsDirection) {
			xDir1 -= first1X;
			yDir1 -= first1Y;
			xDir2 -= first2X;
			yDir2 -= first2Y;
		}
		float d = xDir1 * yDir2 - xDir2 * yDir1;

		if (d == 0) {
			float crs = first1X * first2Y - first1Y * first2X;
			if (crs > 0) {
				if (intersection != null) intersection.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
				return Float.POSITIVE_INFINITY;
			} else if (crs < 0) {
				if (intersection != null) intersection.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
				return Float.NEGATIVE_INFINITY;
			}
		}

		float yd = first1Y - first2Y;
		float xd = first1X - first2X;
		float s1 = -(xd * yDir2 - yd * xDir2) / d;

		if (ub && s1 > 1) {
			if (intersection != null) intersection.set(Float.NaN, Float.NaN);
			return Float.NEGATIVE_INFINITY;
		}
		if ((lb && s1 < 0)) {
			if (intersection != null) intersection.set(Float.NaN, Float.NaN);
			return Float.POSITIVE_INFINITY;
		}
		float s2 = -(xd * yDir1 - yd * xDir1) / d;
		if ((ub && s2 > 1)) {
			if (intersection != null) intersection.set(Float.NaN, Float.NaN);
			return Float.NEGATIVE_INFINITY;
		}
		if ((lb && s2 < 0)) {
			if (intersection != null) intersection.set(Float.NaN, Float.NaN);
			return Float.POSITIVE_INFINITY;
		}

		if (intersection != null) intersection.set(first1X + xDir1 * s1, first1Y + yDir1 * s1);
		return s1;
	}

	/** The cross product of 2 Vectors, defined by (a, b) and (c, d).
	 *
	 * @param a the x-component of the first Vector
	 * @param b the y-component of the first Vector
	 * @param c the x-component of the second Vector
	 * @param d the y-component of the second Vector
	 * @return {@code float} the cross product */
	static float crs (float a, float b, float c, float d) {
		return a * d - b * c;
	}

	/** The dot product of 2 Vectors, defined by (a, b) and (c, d).
	 *
	 * @param a the x-component of the first Vector
	 * @param b the y-component of the first Vector
	 * @param c the x-component of the second Vector
	 * @param d the y-component of the second Vector
	 * @return {@code float} the dot product */
	static float dot (float a, float b, float c, float d) {
		return a * c + b * d;
	}

	public static boolean overlaps (Circle c1, Circle c2) {
		return c1.overlaps(c2);
	}

	public static boolean overlaps (Rectangle r1, Rectangle r2) {
		return r1.overlaps(r2);
	}

	public static boolean overlaps (Circle c, Rectangle r) {
		float closestX = c.x;
		float closestY = c.y;

		if (c.x < r.x) {
			closestX = r.x;
		} else if (c.x > r.x + r.width) {
			closestX = r.x + r.width;
		}

		if (c.y < r.y) {
			closestY = r.y;
		} else if (c.y > r.y + r.height) {
			closestY = r.y + r.height;
		}

		closestX = closestX - c.x;
		closestX *= closestX;
		closestY = closestY - c.y;
		closestY *= closestY;

		return closestX + closestY < c.radius * c.radius;
	}

	/** Check whether specified counter-clockwise wound convex polygons overlap.
	 * 
	 * @param p1 The first polygon.
	 * @param p2 The second polygon.
	 * @return Whether polygons overlap. */
	public static boolean overlapConvexPolygons (Polygon p1, Polygon p2) {
		return overlapConvexPolygons(p1, p2, null);
	}

	/** Check whether specified counter-clockwise wound convex polygons overlap. If they do, optionally obtain a Minimum
	 * Translation Vector indicating the minimum magnitude vector required to push the polygon p1 out of collision with polygon p2.
	 * 
	 * @param p1 The first polygon.
	 * @param p2 The second polygon.
	 * @param mtv A Minimum Translation Vector to fill in the case of a collision, or null (optional).
	 * @return Whether polygons overlap. */
	public static boolean overlapConvexPolygons (Polygon p1, Polygon p2, MinimumTranslationVector mtv) {
		return overlapConvexPolygons(p1.getTransformedVertices(), p2.getTransformedVertices(), mtv);
	}

	/** @see #overlapConvexPolygons(float[], int, int, float[], int, int, MinimumTranslationVector) */
	public static boolean overlapConvexPolygons (float[] verts1, float[] verts2, MinimumTranslationVector mtv) {
		return overlapConvexPolygons(verts1, 0, verts1.length, verts2, 0, verts2.length, mtv);
	}

	/** Check whether polygons defined by the given counter-clockwise wound vertex arrays overlap. If they do, optionally obtain a
	 * Minimum Translation Vector indicating the minimum magnitude vector required to push the polygon defined by verts1 out of the
	 * collision with the polygon defined by verts2.
	 * 
	 * @param verts1 Vertices of the first polygon.
	 * @param verts2 Vertices of the second polygon.
	 * @param mtv A Minimum Translation Vector to fill in the case of a collision, or {@code null} (optional).
	 * @return Whether polygons overlap. */
	public static boolean overlapConvexPolygons (float[] verts1, int offset1, int count1, float[] verts2, int offset2, int count2,
		MinimumTranslationVector mtv) {
		float overlap = Float.MAX_VALUE;
		float smallestAxisX = 0;
		float smallestAxisY = 0;
		int numInNormalDir;

		int end1 = offset1 + count1;
		int end2 = offset2 + count2;

		// Get polygon1 axes
		for (int i = offset1; i < end1; i += 2) {
			float x1 = verts1[i];
			float y1 = verts1[i + 1];
			float x2 = verts1[(i + 2) % count1];
			float y2 = verts1[(i + 3) % count1];

			float axisX = y1 - y2;
			float axisY = -(x1 - x2);

			final float length = (float)Math.sqrt(axisX * axisX + axisY * axisY);
			axisX /= length;
			axisY /= length;

			// -- Begin check for separation on this axis --//

			// Project polygon1 onto this axis
			float min1 = axisX * verts1[0] + axisY * verts1[1];
			float max1 = min1;
			for (int j = offset1; j < end1; j += 2) {
				float p = axisX * verts1[j] + axisY * verts1[j + 1];
				if (p < min1) {
					min1 = p;
				} else if (p > max1) {
					max1 = p;
				}
			}

			// Project polygon2 onto this axis
			numInNormalDir = 0;
			float min2 = axisX * verts2[0] + axisY * verts2[1];
			float max2 = min2;
			for (int j = offset2; j < end2; j += 2) {
				// Counts the number of points that are within the projected area.
				numInNormalDir -= pointLineSide(x1, y1, x2, y2, verts2[j], verts2[j + 1]);
				float p = axisX * verts2[j] + axisY * verts2[j + 1];
				if (p < min2) {
					min2 = p;
				} else if (p > max2) {
					max2 = p;
				}
			}

			if (!(min1 <= min2 && max1 >= min2 || min2 <= min1 && max2 >= min1)) {
				return false;
			} else {
				float o = Math.min(max1, max2) - Math.max(min1, min2);
				if (min1 < min2 && max1 > max2 || min2 < min1 && max2 > max1) {
					float mins = Math.abs(min1 - min2);
					float maxs = Math.abs(max1 - max2);
					if (mins < maxs) {
						o += mins;
					} else {
						o += maxs;
					}
				}
				if (o < overlap) {
					overlap = o;
					// Adjusts the direction based on the number of points found
					smallestAxisX = numInNormalDir >= 0 ? axisX : -axisX;
					smallestAxisY = numInNormalDir >= 0 ? axisY : -axisY;
				}
			}
			// -- End check for separation on this axis --//
		}

		// Get polygon2 axes
		for (int i = offset2; i < end2; i += 2) {
			float x1 = verts2[i];
			float y1 = verts2[i + 1];
			float x2 = verts2[(i + 2) % count2];
			float y2 = verts2[(i + 3) % count2];

			float axisX = y1 - y2;
			float axisY = -(x1 - x2);

			final float length = (float)Math.sqrt(axisX * axisX + axisY * axisY);
			axisX /= length;
			axisY /= length;

			// -- Begin check for separation on this axis --//
			numInNormalDir = 0;

			// Project polygon1 onto this axis
			float min1 = axisX * verts1[0] + axisY * verts1[1];
			float max1 = min1;
			for (int j = offset1; j < end1; j += 2) {
				float p = axisX * verts1[j] + axisY * verts1[j + 1];
				// Counts the number of points that are within the projected area.
				numInNormalDir -= pointLineSide(x1, y1, x2, y2, verts1[j], verts1[j + 1]);
				if (p < min1) {
					min1 = p;
				} else if (p > max1) {
					max1 = p;
				}
			}

			// Project polygon2 onto this axis
			float min2 = axisX * verts2[0] + axisY * verts2[1];
			float max2 = min2;
			for (int j = offset2; j < end2; j += 2) {
				float p = axisX * verts2[j] + axisY * verts2[j + 1];
				if (p < min2) {
					min2 = p;
				} else if (p > max2) {
					max2 = p;
				}
			}

			if (!(min1 <= min2 && max1 >= min2 || min2 <= min1 && max2 >= min1)) {
				return false;
			} else {
				float o = Math.min(max1, max2) - Math.max(min1, min2);

				if (min1 < min2 && max1 > max2 || min2 < min1 && max2 > max1) {
					float mins = Math.abs(min1 - min2);
					float maxs = Math.abs(max1 - max2);
					if (mins < maxs) {
						o += mins;
					} else {
						o += maxs;
					}
				}

				if (o < overlap) {
					overlap = o;
					// Adjusts the direction based on the number of points found
					smallestAxisX = numInNormalDir < 0 ? axisX : -axisX;
					smallestAxisY = numInNormalDir < 0 ? axisY : -axisY;
				}
			}
			// -- End check for separation on this axis --//
		}
		if (mtv != null) {
			mtv.normal.set(smallestAxisX, smallestAxisY);
			mtv.depth = overlap;
		}
		return true;
	}

	/** Splits the triangle by the plane. The result is stored in the SplitTriangle instance. Depending on where the triangle is
	 * relative to the plane, the result can be:
	 * 
	 * <ul>
	 * <li>Triangle is fully in front/behind: {@link SplitTriangle#front} or {@link SplitTriangle#back} will contain the original
	 * triangle, {@link SplitTriangle#total} will be one.</li>
	 * <li>Triangle has two vertices in front, one behind: {@link SplitTriangle#front} contains 2 triangles,
	 * {@link SplitTriangle#back} contains 1 triangles, {@link SplitTriangle#total} will be 3.</li>
	 * <li>Triangle has one vertex in front, two behind: {@link SplitTriangle#front} contains 1 triangle,
	 * {@link SplitTriangle#back} contains 2 triangles, {@link SplitTriangle#total} will be 3.</li>
	 * </ul>
	 * 
	 * The input triangle should have the form: x, y, z, x2, y2, z2, x3, y3, z3. One can add additional attributes per vertex which
	 * will be interpolated if split, such as texture coordinates or normals. Note that these additional attributes won't be
	 * normalized, as might be necessary in case of normals.
	 * 
	 * @param triangle
	 * @param plane
	 * @param split output SplitTriangle */
	public static void splitTriangle (float[] triangle, Plane plane, SplitTriangle split) {
		int stride = triangle.length / 3;
		boolean r1 = plane.testPoint(triangle[0], triangle[1], triangle[2]) == PlaneSide.Back;
		boolean r2 = plane.testPoint(triangle[0 + stride], triangle[1 + stride], triangle[2 + stride]) == PlaneSide.Back;
		boolean r3 = plane.testPoint(triangle[0 + stride * 2], triangle[1 + stride * 2],
			triangle[2 + stride * 2]) == PlaneSide.Back;

		split.reset();

		// easy case, triangle is on one side (point on plane means front).
		if (r1 == r2 && r2 == r3) {
			split.total = 1;
			if (r1) {
				split.numBack = 1;
				System.arraycopy(triangle, 0, split.back, 0, triangle.length);
			} else {
				split.numFront = 1;
				System.arraycopy(triangle, 0, split.front, 0, triangle.length);
			}
			return;
		}

		// set number of triangles
		split.total = 3;
		split.numFront = (r1 ? 0 : 1) + (r2 ? 0 : 1) + (r3 ? 0 : 1);
		split.numBack = split.total - split.numFront;

		// hard case, split the three edges on the plane
		// determine which array to fill first, front or back, flip if we
		// cross the plane
		split.setSide(!r1);

		// split first edge
		int first = 0;
		int second = stride;
		if (r1 != r2) {
			// split the edge
			splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);

			// add first edge vertex and new vertex to current side
			split.add(triangle, first, stride);
			split.add(split.edgeSplit, 0, stride);

			// flip side and add new vertex and second edge vertex to current side
			split.setSide(!split.getSide());
			split.add(split.edgeSplit, 0, stride);
		} else {
			// add both vertices
			split.add(triangle, first, stride);
		}

		// split second edge
		first = stride;
		second = stride + stride;
		if (r2 != r3) {
			// split the edge
			splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);

			// add first edge vertex and new vertex to current side
			split.add(triangle, first, stride);
			split.add(split.edgeSplit, 0, stride);

			// flip side and add new vertex and second edge vertex to current side
			split.setSide(!split.getSide());
			split.add(split.edgeSplit, 0, stride);
		} else {
			// add both vertices
			split.add(triangle, first, stride);
		}

		// split third edge
		first = stride + stride;
		second = 0;
		if (r3 != r1) {
			// split the edge
			splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);

			// add first edge vertex and new vertex to current side
			split.add(triangle, first, stride);
			split.add(split.edgeSplit, 0, stride);

			// flip side and add new vertex and second edge vertex to current side
			split.setSide(!split.getSide());
			split.add(split.edgeSplit, 0, stride);
		} else {
			// add both vertices
			split.add(triangle, first, stride);
		}

		// triangulate the side with 2 triangles
		if (split.numFront == 2) {
			System.arraycopy(split.front, stride * 2, split.front, stride * 3, stride * 2);
			System.arraycopy(split.front, 0, split.front, stride * 5, stride);
		} else {
			System.arraycopy(split.back, stride * 2, split.back, stride * 3, stride * 2);
			System.arraycopy(split.back, 0, split.back, stride * 5, stride);
		}
	}

	static Vector3 intersection = new Vector3();

	private static void splitEdge (float[] vertices, int s, int e, int stride, Plane plane, float[] split, int offset) {
		float t = Intersector.intersectLinePlane(vertices[s], vertices[s + 1], vertices[s + 2], vertices[e], vertices[e + 1],
			vertices[e + 2], plane, intersection);
		split[offset + 0] = intersection.x;
		split[offset + 1] = intersection.y;
		split[offset + 2] = intersection.z;
		for (int i = 3; i < stride; i++) {
			float a = vertices[s + i];
			float b = vertices[e + i];
			split[offset + i] = a + t * (b - a);
		}
	}

	public static class SplitTriangle {
		public float[] front;
		public float[] back;
		float[] edgeSplit;
		public int numFront;
		public int numBack;
		public int total;
		boolean frontCurrent = false;
		int frontOffset = 0;
		int backOffset = 0;

		/** Creates a new instance, assuming numAttributes attributes per triangle vertex.
		 * 
		 * @param numAttributes must be >= 3 */
		public SplitTriangle (int numAttributes) {
			front = new float[numAttributes * 3 * 2];
			back = new float[numAttributes * 3 * 2];
			edgeSplit = new float[numAttributes];
		}

		@Override
		public String toString () {
			return "SplitTriangle [front=" + Arrays.toString(front) + ", back=" + Arrays.toString(back) + ", numFront=" + numFront
				+ ", numBack=" + numBack + ", total=" + total + "]";
		}

		void setSide (boolean front) {
			frontCurrent = front;
		}

		boolean getSide () {
			return frontCurrent;
		}

		void add (float[] vertex, int offset, int stride) {
			if (frontCurrent) {
				System.arraycopy(vertex, offset, front, frontOffset, stride);
				frontOffset += stride;
			} else {
				System.arraycopy(vertex, offset, back, backOffset, stride);
				backOffset += stride;
			}
		}

		void reset () {
			frontCurrent = false;
			frontOffset = 0;
			backOffset = 0;
			numFront = 0;
			numBack = 0;
			total = 0;
		}
	}

	/** Minimum translation required to separate two polygons. */
	public static class MinimumTranslationVector {
		/** Unit length vector that indicates the direction for the separation */
		public Vector2 normal = new Vector2();
		/** Distance of the translation required for the separation */
		public float depth = 0;
	}
}
