package org.quuux.touchcast.ui;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Pair;

import org.quuux.touchcast.GameActivity;
import org.quuux.touchcast.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* Created by marc on 4/26/14.
*/
public class Recognizer {

    private static final String TAG = Log.buildTag(Recognizer.class);

    static final int NUM_POINTS = 64;
    static final PointF ORIGIN = new PointF(0, 0);
    static final float angleRange = deg2Rad(45);
    static final float anglePrecision = deg2Rad(2);
    static final float PHI = (float) (0.5f * (-1.0f + Math.sqrt(5f))); // Golden Ratio

    final List<UniStroke> mUnistrokes = new ArrayList<UniStroke>();

    class UniStroke {
        final String name;
        final PointF[] points;
        private final float[] vector;

        UniStroke(final String name, PointF[] points) {
            this.name = name;
            points = resample(points, NUM_POINTS);
            float radians = indicativeAngle(points);
            points = rotateBy(points, -radians);
            points = scaleTo(points, 250);
            this.points = translateTo(points, ORIGIN);
            this.vector = vectorize(this.points); // for Protractor
        }
    }

    public Recognizer() {
        init();
    }

    Pair<UniStroke, Float> recognize(PointF[] rawPoints, final boolean useProtractor) {

        PointF[] points = resample(rawPoints, NUM_POINTS);


        float radians = indicativeAngle(points);
        points = rotateBy(points, -radians);
        points = scaleTo(points, 250f);
        points = translateTo(points, ORIGIN);

        float[] vector = vectorize(points); // for Protractor

        float b = Float.POSITIVE_INFINITY;
        int u = -1;
        for (int i = 0; i < mUnistrokes.size(); i++) // for each unistroke
        {
            float d;

            final UniStroke uni = mUnistrokes.get(i);

            if (useProtractor) // for Protractor
                d = optimalCosineDistance(uni.vector, vector);
            else // Golden Section Search (original $1)
                d = distanceAtBestAngle(points, uni, -angleRange, +angleRange, anglePrecision);

            Log.d(TAG, "comparing %s -> %s", uni.name, d);

            if (d < b) {
                b = d; // best (least) distance
                u = i; // unistroke
            }
        }

        final float score = (u == -1) ? 0 : 1f / b;
        final UniStroke stroke = mUnistrokes.get(u);
        return new Pair<UniStroke, Float>(stroke, score);

    }

    static float distanceAtBestAngle(final PointF[] points, final UniStroke T, float a, float b, final float threshold) {
        float x1 = PHI * a + (1f - PHI) * b;
        float f1 = distanceAtAngle(points, T, x1);
        float x2 = (1f - PHI) * a + PHI * b;
        float f2 = distanceAtAngle(points, T, x2);
        while (Math.abs(b - a) > threshold) {
            if (f1 < f2) {
                b = x2;
                x2 = x1;
                f2 = f1;
                x1 = PHI * a + (1 - PHI) * b;
                f1 = distanceAtAngle(points, T, x1);
            } else {
                a = x1;
                x1 = x2;
                f1 = f2;
                x2 = (-PHI) * a + PHI * b;
                f2 = distanceAtAngle(points, T, x2);
            }
        }
        return Math.min(f1, f2);
    }

    static float distanceAtAngle(final PointF[] points, final UniStroke T, final float radians) {
        final PointF[] newpoints = rotateBy(points, radians);
        return pathDistance(newpoints, T.points);
    }

    static float optimalCosineDistance(final float[] v1, final float[] v2) {
        float a = 0;
        float b = 0;
        for (int i = 0; i < v1.length; i += 2) {
            a += v1[i] * v2[i] + v1[i + 1] * v2[i + 1];
            b += v1[i] * v2[i + 1] - v1[i + 1] * v2[i];
        }

        float angle = (float) Math.atan(b / a);

        return (float) Math.acos(a * Math.cos(angle) + b * Math.sin(angle));
    }

    static float[] vectorize(final PointF[] points) {
        float sum = 0;

        List<Float> vector = new ArrayList<Float>();

        for (int i = 0; i < points.length; i++) {
            vector.add(points[i].x);
            vector.add(points[i].y);
            sum += points[i].x * points[i].x + points[i].y * points[i].y;
        }

        final float rv[] = new float[vector.size()];
        float magnitude = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.size(); i++)
            rv[i] = vector.get(i) / magnitude;

        return rv;
    }

    static PointF[] translateTo(final PointF[] points, final PointF point) {
        final PointF c = centroid(points);
        final PointF[] newpoints = new PointF[points.length];
        for (int i = 0; i < points.length; i++) {
            float qx = points[i].x + point.x - c.x;
            float qy = points[i].y + point.y - c.y;
            newpoints[i] = new PointF(qx, qy);
        }
        return newpoints;
    }

    static PointF[] scaleTo(final PointF[] points, final float scale) {
        RectF B = boundingBox(points);
        final PointF[] newpoints = new PointF[points.length];
        for (int i = 0; i < points.length; i++) {
            float qx = points[i].x * (scale / B.width());
            float qy = points[i].y * (scale / B.height());
            newpoints[i] = new PointF(qx, qy);
        }
        return newpoints;
    }


    public static  RectF boundingBox(final PointF[] points) {
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY,
                minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < points.length; i++) {
            minX = Math.min(minX, points[i].x);
            minY = Math.min(minY, points[i].y);
            maxX = Math.max(maxX, points[i].x);
            maxY = Math.max(maxY, points[i].y);
        }
        return new RectF(minX, minY, maxX, maxY);
    }

    static PointF[] rotateBy(final PointF[] points, final float radians) {
        final PointF c = centroid(points);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        PointF[] newpoints = new PointF[points.length];
        for (int i = 0; i < points.length; i++) {
            float qx = (points[i].x - c.x) * cos - (points[i].y - c.y) * sin + c.x;
            float qy = (points[i].x - c.x) * sin + (points[i].y - c.y) * cos + c.y;
            newpoints[i] = new PointF(qx, qy);
        }
        return newpoints;
    }

    static PointF[] resample(final PointF[] points, final int numPoints) {
        float I = pathLength(points) / (numPoints - 1);
        float D = 0;

        final ArrayList<PointF> pointsList = new ArrayList<PointF>(Arrays.asList(points));
        final List<PointF> resampled = new ArrayList<PointF>();

        resampled.add(pointsList.get(0));

        for (int i = 1; i < pointsList.size(); i++) {
            float d = distance(pointsList.get(i - 1), pointsList.get(i));
            if ((D + d) >= I) {
                float qx = pointsList.get(i - 1).x + ((I - D) / d) * (pointsList.get(i).x - pointsList.get(i - 1).x);
                float qy = pointsList.get(i - 1).y + ((I - D) / d) * (pointsList.get(i).y - pointsList.get(i - 1).y);
                final PointF q = new PointF(qx, qy);
                resampled.add(q);

                pointsList.add(i, q); // insert 'q' at position i in points s.t. 'q' will be the next i

                D = 0;
            } else {
                D += d;
            }
        }

        if (resampled.size() == numPoints - 1) // sometimes we fall a rounding-error short of adding the last point, so add it if so
            resampled.add(new PointF(points[points.length - 1].x, points[points.length - 1].y));

        return resampled.toArray(new PointF[resampled.size()]);
    }

    static float indicativeAngle(final PointF[] points) {
        final PointF c = centroid(points);
        return (float) Math.atan2(c.y - points[0].y, c.x - points[0].x);
    }

    static PointF centroid(final PointF[] points) {
        float x = 0, y = 0;
        for (int i = 0; i < points.length; i++) {
            x += points[i].x;
            y += points[i].y;
        }
        x /= points.length;
        y /= points.length;
        return new PointF(x, y);
    }

    static float pathLength(final PointF[] points) {
        float d = 0;
        for (int i = 1; i < points.length; i++)
            d += distance(points[i - 1], points[i]);
        return d;
    }

    static float pathDistance(final PointF[] pts1, final PointF[] pts2) {
        float d = 0;
        for (int i = 0; i < pts1.length; i++) // assumes pts1.length == pts2.length
            d += distance(pts1[i], pts2[i]);
        return d / pts1.length;
    }

    static float distance(final PointF p1, final PointF p2) {
        final float dx = p2.x - p1.x;
        final float dy = p2.y - p1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    static float deg2Rad(final float d) {
        return (float) (d * Math.PI / 180.0);
    }

    void init() {
        final UniStroke[] strokes = new UniStroke[]{
                new UniStroke("triangle", new PointF[] {new PointF(137, 139), new PointF(135, 141), new PointF(133, 144), new PointF(132, 146), new PointF(130, 149), new PointF(128, 151), new PointF(126, 155), new PointF(123, 160), new PointF(120, 166), new PointF(116, 171), new PointF(112, 177), new PointF(107, 183), new PointF(102, 188), new PointF(100, 191), new PointF(95, 195), new PointF(90, 199), new PointF(86, 203), new PointF(82, 206), new PointF(80, 209), new PointF(75, 213), new PointF(73, 213), new PointF(70, 216), new PointF(67, 219), new PointF(64, 221), new PointF(61, 223), new PointF(60, 225), new PointF(62, 226), new PointF(65, 225), new PointF(67, 226), new PointF(74, 226), new PointF(77, 227), new PointF(85, 229), new PointF(91, 230), new PointF(99, 231), new PointF(108, 232), new PointF(116, 233), new PointF(125, 233), new PointF(134, 234), new PointF(145, 233), new PointF(153, 232), new PointF(160, 233), new PointF(170, 234), new PointF(177, 235), new PointF(179, 236), new PointF(186, 237), new PointF(193, 238), new PointF(198, 239), new PointF(200, 237), new PointF(202, 239), new PointF(204, 238), new PointF(206, 234), new PointF(205, 230), new PointF(202, 222), new PointF(197, 216), new PointF(192, 207), new PointF(186, 198), new PointF(179, 189), new PointF(174, 183), new PointF(170, 178), new PointF(164, 171), new PointF(161, 168), new PointF(154, 160), new PointF(148, 155), new PointF(143, 150), new PointF(138, 148), new PointF(136, 148)}),
                new UniStroke("x", new PointF[] {new PointF(87, 142), new PointF(89, 145), new PointF(91, 148), new PointF(93, 151), new PointF(96, 155), new PointF(98, 157), new PointF(100, 160), new PointF(102, 162), new PointF(106, 167), new PointF(108, 169), new PointF(110, 171), new PointF(115, 177), new PointF(119, 183), new PointF(123, 189), new PointF(127, 193), new PointF(129, 196), new PointF(133, 200), new PointF(137, 206), new PointF(140, 209), new PointF(143, 212), new PointF(146, 215), new PointF(151, 220), new PointF(153, 222), new PointF(155, 223), new PointF(157, 225), new PointF(158, 223), new PointF(157, 218), new PointF(155, 211), new PointF(154, 208), new PointF(152, 200), new PointF(150, 189), new PointF(148, 179), new PointF(147, 170), new PointF(147, 158), new PointF(147, 148), new PointF(147, 141), new PointF(147, 136), new PointF(144, 135), new PointF(142, 137), new PointF(140, 139), new PointF(135, 145), new PointF(131, 152), new PointF(124, 163), new PointF(116, 177), new PointF(108, 191), new PointF(100, 206), new PointF(94, 217), new PointF(91, 222), new PointF(89, 225), new PointF(87, 226), new PointF(87, 224)}),
                new UniStroke("rectangle", new PointF[] {new PointF(78, 149), new PointF(78, 153), new PointF(78, 157), new PointF(78, 160), new PointF(79, 162), new PointF(79, 164), new PointF(79, 167), new PointF(79, 169), new PointF(79, 173), new PointF(79, 178), new PointF(79, 183), new PointF(80, 189), new PointF(80, 193), new PointF(80, 198), new PointF(80, 202), new PointF(81, 208), new PointF(81, 210), new PointF(81, 216), new PointF(82, 222), new PointF(82, 224), new PointF(82, 227), new PointF(83, 229), new PointF(83, 231), new PointF(85, 230), new PointF(88, 232), new PointF(90, 233), new PointF(92, 232), new PointF(94, 233), new PointF(99, 232), new PointF(102, 233), new PointF(106, 233), new PointF(109, 234), new PointF(117, 235), new PointF(123, 236), new PointF(126, 236), new PointF(135, 237), new PointF(142, 238), new PointF(145, 238), new PointF(152, 238), new PointF(154, 239), new PointF(165, 238), new PointF(174, 237), new PointF(179, 236), new PointF(186, 235), new PointF(191, 235), new PointF(195, 233), new PointF(197, 233), new PointF(200, 233), new PointF(201, 235), new PointF(201, 233), new PointF(199, 231), new PointF(198, 226), new PointF(198, 220), new PointF(196, 207), new PointF(195, 195), new PointF(195, 181), new PointF(195, 173), new PointF(195, 163), new PointF(194, 155), new PointF(192, 145), new PointF(192, 143), new PointF(192, 138), new PointF(191, 135), new PointF(191, 133), new PointF(191, 130), new PointF(190, 128), new PointF(188, 129), new PointF(186, 129), new PointF(181, 132), new PointF(173, 131), new PointF(162, 131), new PointF(151, 132), new PointF(149, 132), new PointF(138, 132), new PointF(136, 132), new PointF(122, 131), new PointF(120, 131), new PointF(109, 130), new PointF(107, 130), new PointF(90, 132), new PointF(81, 133), new PointF(76, 133)}),
                new UniStroke("circle", new PointF[] {new PointF(127, 141), new PointF(124, 140), new PointF(120, 139), new PointF(118, 139), new PointF(116, 139), new PointF(111, 140), new PointF(109, 141), new PointF(104, 144), new PointF(100, 147), new PointF(96, 152), new PointF(93, 157), new PointF(90, 163), new PointF(87, 169), new PointF(85, 175), new PointF(83, 181), new PointF(82, 190), new PointF(82, 195), new PointF(83, 200), new PointF(84, 205), new PointF(88, 213), new PointF(91, 216), new PointF(96, 219), new PointF(103, 222), new PointF(108, 224), new PointF(111, 224), new PointF(120, 224), new PointF(133, 223), new PointF(142, 222), new PointF(152, 218), new PointF(160, 214), new PointF(167, 210), new PointF(173, 204), new PointF(178, 198), new PointF(179, 196), new PointF(182, 188), new PointF(182, 177), new PointF(178, 167), new PointF(170, 150), new PointF(163, 138), new PointF(152, 130), new PointF(143, 129), new PointF(140, 131), new PointF(129, 136), new PointF(126, 139)}),
                new UniStroke("check", new PointF[] {new PointF(91, 185), new PointF(93, 185), new PointF(95, 185), new PointF(97, 185), new PointF(100, 188), new PointF(102, 189), new PointF(104, 190), new PointF(106, 193), new PointF(108, 195), new PointF(110, 198), new PointF(112, 201), new PointF(114, 204), new PointF(115, 207), new PointF(117, 210), new PointF(118, 212), new PointF(120, 214), new PointF(121, 217), new PointF(122, 219), new PointF(123, 222), new PointF(124, 224), new PointF(126, 226), new PointF(127, 229), new PointF(129, 231), new PointF(130, 233), new PointF(129, 231), new PointF(129, 228), new PointF(129, 226), new PointF(129, 224), new PointF(129, 221), new PointF(129, 218), new PointF(129, 212), new PointF(129, 208), new PointF(130, 198), new PointF(132, 189), new PointF(134, 182), new PointF(137, 173), new PointF(143, 164), new PointF(147, 157), new PointF(151, 151), new PointF(155, 144), new PointF(161, 137), new PointF(165, 131), new PointF(171, 122), new PointF(174, 118), new PointF(176, 114), new PointF(177, 112), new PointF(177, 114), new PointF(175, 116), new PointF(173, 118)}),
                new UniStroke("caret", new PointF[] {new PointF(79, 245), new PointF(79, 242), new PointF(79, 239), new PointF(80, 237), new PointF(80, 234), new PointF(81, 232), new PointF(82, 230), new PointF(84, 224), new PointF(86, 220), new PointF(86, 218), new PointF(87, 216), new PointF(88, 213), new PointF(90, 207), new PointF(91, 202), new PointF(92, 200), new PointF(93, 194), new PointF(94, 192), new PointF(96, 189), new PointF(97, 186), new PointF(100, 179), new PointF(102, 173), new PointF(105, 165), new PointF(107, 160), new PointF(109, 158), new PointF(112, 151), new PointF(115, 144), new PointF(117, 139), new PointF(119, 136), new PointF(119, 134), new PointF(120, 132), new PointF(121, 129), new PointF(122, 127), new PointF(124, 125), new PointF(126, 124), new PointF(129, 125), new PointF(131, 127), new PointF(132, 130), new PointF(136, 139), new PointF(141, 154), new PointF(145, 166), new PointF(151, 182), new PointF(156, 193), new PointF(157, 196), new PointF(161, 209), new PointF(162, 211), new PointF(167, 223), new PointF(169, 229), new PointF(170, 231), new PointF(173, 237), new PointF(176, 242), new PointF(177, 244), new PointF(179, 250), new PointF(181, 255), new PointF(182, 257)}),
                new UniStroke("zig-zag", new PointF[] {new PointF(307, 216), new PointF(333, 186), new PointF(356, 215), new PointF(375, 186), new PointF(399, 216), new PointF(418, 186)}),
                new UniStroke("arrow", new PointF[] {new PointF(68, 222), new PointF(70, 220), new PointF(73, 218), new PointF(75, 217), new PointF(77, 215), new PointF(80, 213), new PointF(82, 212), new PointF(84, 210), new PointF(87, 209), new PointF(89, 208), new PointF(92, 206), new PointF(95, 204), new PointF(101, 201), new PointF(106, 198), new PointF(112, 194), new PointF(118, 191), new PointF(124, 187), new PointF(127, 186), new PointF(132, 183), new PointF(138, 181), new PointF(141, 180), new PointF(146, 178), new PointF(154, 173), new PointF(159, 171), new PointF(161, 170), new PointF(166, 167), new PointF(168, 167), new PointF(171, 166), new PointF(174, 164), new PointF(177, 162), new PointF(180, 160), new PointF(182, 158), new PointF(183, 156), new PointF(181, 154), new PointF(178, 153), new PointF(171, 153), new PointF(164, 153), new PointF(160, 153), new PointF(150, 154), new PointF(147, 155), new PointF(141, 157), new PointF(137, 158), new PointF(135, 158), new PointF(137, 158), new PointF(140, 157), new PointF(143, 156), new PointF(151, 154), new PointF(160, 152), new PointF(170, 149), new PointF(179, 147), new PointF(185, 145), new PointF(192, 144), new PointF(196, 144), new PointF(198, 144), new PointF(200, 144), new PointF(201, 147), new PointF(199, 149), new PointF(194, 157), new PointF(191, 160), new PointF(186, 167), new PointF(180, 176), new PointF(177, 179), new PointF(171, 187), new PointF(169, 189), new PointF(165, 194), new PointF(164, 196)}),
                new UniStroke("left square bracket", new PointF[] {new PointF(140, 124), new PointF(138, 123), new PointF(135, 122), new PointF(133, 123), new PointF(130, 123), new PointF(128, 124), new PointF(125, 125), new PointF(122, 124), new PointF(120, 124), new PointF(118, 124), new PointF(116, 125), new PointF(113, 125), new PointF(111, 125), new PointF(108, 124), new PointF(106, 125), new PointF(104, 125), new PointF(102, 124), new PointF(100, 123), new PointF(98, 123), new PointF(95, 124), new PointF(93, 123), new PointF(90, 124), new PointF(88, 124), new PointF(85, 125), new PointF(83, 126), new PointF(81, 127), new PointF(81, 129), new PointF(82, 131), new PointF(82, 134), new PointF(83, 138), new PointF(84, 141), new PointF(84, 144), new PointF(85, 148), new PointF(85, 151), new PointF(86, 156), new PointF(86, 160), new PointF(86, 164), new PointF(86, 168), new PointF(87, 171), new PointF(87, 175), new PointF(87, 179), new PointF(87, 182), new PointF(87, 186), new PointF(88, 188), new PointF(88, 195), new PointF(88, 198), new PointF(88, 201), new PointF(88, 207), new PointF(89, 211), new PointF(89, 213), new PointF(89, 217), new PointF(89, 222), new PointF(88, 225), new PointF(88, 229), new PointF(88, 231), new PointF(88, 233), new PointF(88, 235), new PointF(89, 237), new PointF(89, 240), new PointF(89, 242), new PointF(91, 241), new PointF(94, 241), new PointF(96, 240), new PointF(98, 239), new PointF(105, 240), new PointF(109, 240), new PointF(113, 239), new PointF(116, 240), new PointF(121, 239), new PointF(130, 240), new PointF(136, 237), new PointF(139, 237), new PointF(144, 238), new PointF(151, 237), new PointF(157, 236), new PointF(159, 237)}),
                new UniStroke("right square bracket", new PointF[] {new PointF(112, 138), new PointF(112, 136), new PointF(115, 136), new PointF(118, 137), new PointF(120, 136), new PointF(123, 136), new PointF(125, 136), new PointF(128, 136), new PointF(131, 136), new PointF(134, 135), new PointF(137, 135), new PointF(140, 134), new PointF(143, 133), new PointF(145, 132), new PointF(147, 132), new PointF(149, 132), new PointF(152, 132), new PointF(153, 134), new PointF(154, 137), new PointF(155, 141), new PointF(156, 144), new PointF(157, 152), new PointF(158, 161), new PointF(160, 170), new PointF(162, 182), new PointF(164, 192), new PointF(166, 200), new PointF(167, 209), new PointF(168, 214), new PointF(168, 216), new PointF(169, 221), new PointF(169, 223), new PointF(169, 228), new PointF(169, 231), new PointF(166, 233), new PointF(164, 234), new PointF(161, 235), new PointF(155, 236), new PointF(147, 235), new PointF(140, 233), new PointF(131, 233), new PointF(124, 233), new PointF(117, 235), new PointF(114, 238), new PointF(112, 238)}),
                new UniStroke("v", new PointF[] {new PointF(89, 164), new PointF(90, 162), new PointF(92, 162), new PointF(94, 164), new PointF(95, 166), new PointF(96, 169), new PointF(97, 171), new PointF(99, 175), new PointF(101, 178), new PointF(103, 182), new PointF(106, 189), new PointF(108, 194), new PointF(111, 199), new PointF(114, 204), new PointF(117, 209), new PointF(119, 214), new PointF(122, 218), new PointF(124, 222), new PointF(126, 225), new PointF(128, 228), new PointF(130, 229), new PointF(133, 233), new PointF(134, 236), new PointF(136, 239), new PointF(138, 240), new PointF(139, 242), new PointF(140, 244), new PointF(142, 242), new PointF(142, 240), new PointF(142, 237), new PointF(143, 235), new PointF(143, 233), new PointF(145, 229), new PointF(146, 226), new PointF(148, 217), new PointF(149, 208), new PointF(149, 205), new PointF(151, 196), new PointF(151, 193), new PointF(153, 182), new PointF(155, 172), new PointF(157, 165), new PointF(159, 160), new PointF(162, 155), new PointF(164, 150), new PointF(165, 148), new PointF(166, 146)}),
                new UniStroke("delete", new PointF[] {new PointF(123, 129), new PointF(123, 131), new PointF(124, 133), new PointF(125, 136), new PointF(127, 140), new PointF(129, 142), new PointF(133, 148), new PointF(137, 154), new PointF(143, 158), new PointF(145, 161), new PointF(148, 164), new PointF(153, 170), new PointF(158, 176), new PointF(160, 178), new PointF(164, 183), new PointF(168, 188), new PointF(171, 191), new PointF(175, 196), new PointF(178, 200), new PointF(180, 202), new PointF(181, 205), new PointF(184, 208), new PointF(186, 210), new PointF(187, 213), new PointF(188, 215), new PointF(186, 212), new PointF(183, 211), new PointF(177, 208), new PointF(169, 206), new PointF(162, 205), new PointF(154, 207), new PointF(145, 209), new PointF(137, 210), new PointF(129, 214), new PointF(122, 217), new PointF(118, 218), new PointF(111, 221), new PointF(109, 222), new PointF(110, 219), new PointF(112, 217), new PointF(118, 209), new PointF(120, 207), new PointF(128, 196), new PointF(135, 187), new PointF(138, 183), new PointF(148, 167), new PointF(157, 153), new PointF(163, 145), new PointF(165, 142), new PointF(172, 133), new PointF(177, 127), new PointF(179, 127), new PointF(180, 125)}),
                new UniStroke("left curly brace", new PointF[] {new PointF(150, 116), new PointF(147, 117), new PointF(145, 116), new PointF(142, 116), new PointF(139, 117), new PointF(136, 117), new PointF(133, 118), new PointF(129, 121), new PointF(126, 122), new PointF(123, 123), new PointF(120, 125), new PointF(118, 127), new PointF(115, 128), new PointF(113, 129), new PointF(112, 131), new PointF(113, 134), new PointF(115, 134), new PointF(117, 135), new PointF(120, 135), new PointF(123, 137), new PointF(126, 138), new PointF(129, 140), new PointF(135, 143), new PointF(137, 144), new PointF(139, 147), new PointF(141, 149), new PointF(140, 152), new PointF(139, 155), new PointF(134, 159), new PointF(131, 161), new PointF(124, 166), new PointF(121, 166), new PointF(117, 166), new PointF(114, 167), new PointF(112, 166), new PointF(114, 164), new PointF(116, 163), new PointF(118, 163), new PointF(120, 162), new PointF(122, 163), new PointF(125, 164), new PointF(127, 165), new PointF(129, 166), new PointF(130, 168), new PointF(129, 171), new PointF(127, 175), new PointF(125, 179), new PointF(123, 184), new PointF(121, 190), new PointF(120, 194), new PointF(119, 199), new PointF(120, 202), new PointF(123, 207), new PointF(127, 211), new PointF(133, 215), new PointF(142, 219), new PointF(148, 220), new PointF(151, 221)}),
                new UniStroke("right curly brace", new PointF[] {new PointF(117, 132), new PointF(115, 132), new PointF(115, 129), new PointF(117, 129), new PointF(119, 128), new PointF(122, 127), new PointF(125, 127), new PointF(127, 127), new PointF(130, 127), new PointF(133, 129), new PointF(136, 129), new PointF(138, 130), new PointF(140, 131), new PointF(143, 134), new PointF(144, 136), new PointF(145, 139), new PointF(145, 142), new PointF(145, 145), new PointF(145, 147), new PointF(145, 149), new PointF(144, 152), new PointF(142, 157), new PointF(141, 160), new PointF(139, 163), new PointF(137, 166), new PointF(135, 167), new PointF(133, 169), new PointF(131, 172), new PointF(128, 173), new PointF(126, 176), new PointF(125, 178), new PointF(125, 180), new PointF(125, 182), new PointF(126, 184), new PointF(128, 187), new PointF(130, 187), new PointF(132, 188), new PointF(135, 189), new PointF(140, 189), new PointF(145, 189), new PointF(150, 187), new PointF(155, 186), new PointF(157, 185), new PointF(159, 184), new PointF(156, 185), new PointF(154, 185), new PointF(149, 185), new PointF(145, 187), new PointF(141, 188), new PointF(136, 191), new PointF(134, 191), new PointF(131, 192), new PointF(129, 193), new PointF(129, 195), new PointF(129, 197), new PointF(131, 200), new PointF(133, 202), new PointF(136, 206), new PointF(139, 211), new PointF(142, 215), new PointF(145, 220), new PointF(147, 225), new PointF(148, 231), new PointF(147, 239), new PointF(144, 244), new PointF(139, 248), new PointF(134, 250), new PointF(126, 253), new PointF(119, 253), new PointF(115, 253)}),
                new UniStroke("star", new PointF[] {new PointF(75, 250), new PointF(75, 247), new PointF(77, 244), new PointF(78, 242), new PointF(79, 239), new PointF(80, 237), new PointF(82, 234), new PointF(82, 232), new PointF(84, 229), new PointF(85, 225), new PointF(87, 222), new PointF(88, 219), new PointF(89, 216), new PointF(91, 212), new PointF(92, 208), new PointF(94, 204), new PointF(95, 201), new PointF(96, 196), new PointF(97, 194), new PointF(98, 191), new PointF(100, 185), new PointF(102, 178), new PointF(104, 173), new PointF(104, 171), new PointF(105, 164), new PointF(106, 158), new PointF(107, 156), new PointF(107, 152), new PointF(108, 145), new PointF(109, 141), new PointF(110, 139), new PointF(112, 133), new PointF(113, 131), new PointF(116, 127), new PointF(117, 125), new PointF(119, 122), new PointF(121, 121), new PointF(123, 120), new PointF(125, 122), new PointF(125, 125), new PointF(127, 130), new PointF(128, 133), new PointF(131, 143), new PointF(136, 153), new PointF(140, 163), new PointF(144, 172), new PointF(145, 175), new PointF(151, 189), new PointF(156, 201), new PointF(161, 213), new PointF(166, 225), new PointF(169, 233), new PointF(171, 236), new PointF(174, 243), new PointF(177, 247), new PointF(178, 249), new PointF(179, 251), new PointF(180, 253), new PointF(180, 255), new PointF(179, 257), new PointF(177, 257), new PointF(174, 255), new PointF(169, 250), new PointF(164, 247), new PointF(160, 245), new PointF(149, 238), new PointF(138, 230), new PointF(127, 221), new PointF(124, 220), new PointF(112, 212), new PointF(110, 210), new PointF(96, 201), new PointF(84, 195), new PointF(74, 190), new PointF(64, 182), new PointF(55, 175), new PointF(51, 172), new PointF(49, 170), new PointF(51, 169), new PointF(56, 169), new PointF(66, 169), new PointF(78, 168), new PointF(92, 166), new PointF(107, 164), new PointF(123, 161), new PointF(140, 162), new PointF(156, 162), new PointF(171, 160), new PointF(173, 160), new PointF(186, 160), new PointF(195, 160), new PointF(198, 161), new PointF(203, 163), new PointF(208, 163), new PointF(206, 164), new PointF(200, 167), new PointF(187, 172), new PointF(174, 179), new PointF(172, 181), new PointF(153, 192), new PointF(137, 201), new PointF(123, 211), new PointF(112, 220), new PointF(99, 229), new PointF(90, 237), new PointF(80, 244), new PointF(73, 250), new PointF(69, 254), new PointF(69, 252)}),
                new UniStroke("pigtail", new PointF[] {new PointF(81, 219), new PointF(84, 218), new PointF(86, 220), new PointF(88, 220), new PointF(90, 220), new PointF(92, 219), new PointF(95, 220), new PointF(97, 219), new PointF(99, 220), new PointF(102, 218), new PointF(105, 217), new PointF(107, 216), new PointF(110, 216), new PointF(113, 214), new PointF(116, 212), new PointF(118, 210), new PointF(121, 208), new PointF(124, 205), new PointF(126, 202), new PointF(129, 199), new PointF(132, 196), new PointF(136, 191), new PointF(139, 187), new PointF(142, 182), new PointF(144, 179), new PointF(146, 174), new PointF(148, 170), new PointF(149, 168), new PointF(151, 162), new PointF(152, 160), new PointF(152, 157), new PointF(152, 155), new PointF(152, 151), new PointF(152, 149), new PointF(152, 146), new PointF(149, 142), new PointF(148, 139), new PointF(145, 137), new PointF(141, 135), new PointF(139, 135), new PointF(134, 136), new PointF(130, 140), new PointF(128, 142), new PointF(126, 145), new PointF(122, 150), new PointF(119, 158), new PointF(117, 163), new PointF(115, 170), new PointF(114, 175), new PointF(117, 184), new PointF(120, 190), new PointF(125, 199), new PointF(129, 203), new PointF(133, 208), new PointF(138, 213), new PointF(145, 215), new PointF(155, 218), new PointF(164, 219), new PointF(166, 219), new PointF(177, 219), new PointF(182, 218), new PointF(192, 216), new PointF(196, 213), new PointF(199, 212), new PointF(201, 211)})
        };

        for (final UniStroke u : strokes)
            mUnistrokes.add(u);
    }

}
