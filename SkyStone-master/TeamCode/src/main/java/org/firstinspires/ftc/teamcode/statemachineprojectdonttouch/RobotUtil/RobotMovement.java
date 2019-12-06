package org.firstinspires.ftc.teamcode.statemachineprojectdonttouch.RobotUtil;

import org.firstinspires.ftc.teamcode.HelperClasses.ppProject.company.Range;
import org.firstinspires.ftc.teamcode.statemachineprojectdonttouch.HelperClasses.WayPoint;
import org.firstinspires.ftc.teamcode.statemachineprojectdonttouch.HelperClasses.movementTarget;


import java.util.ArrayList;

import static org.firstinspires.ftc.teamcode.HelperClasses.GLOBALS.*;
import static org.firstinspires.ftc.teamcode.statemachineprojectdonttouch.RobotUtil.RobotPosition.*;



import static java.lang.Math.*;

public class RobotMovement {

    private static turnStates ourTurnStates = turnStates.speed;
    private enum turnStates {
        speed,
        accurate
    }

    public static void goToPosition(double targetX, double targetY, double point_angle, double movement_speed, double point_speed) {

        //get our distance away from the point

        double distanceToPoint = Math.sqrt(Math.pow(targetX-scaledWorldXPos,2) + Math.pow(targetY-scaledWorldYPos,2));

        double angleToPoint = Math.atan2(targetY-scaledWorldYPos,targetX-scaledWorldXPos);
        double deltaAngleToPoint = AngleWrap(angleToPoint-(scaledWorldHeadingRad));
        //x and y components required to move toward the next point (with angle correction)
        double relative_x_to_point = Math.cos(deltaAngleToPoint) * distanceToPoint;
        double relative_y_to_point = Math.sin(deltaAngleToPoint) * distanceToPoint;

        double relative_abs_x = Math.abs(relative_x_to_point);
        double relative_abs_y = Math.abs(relative_y_to_point);


        //preserve the shape (ratios) of our intended movement direction but scale it by movement_speed
        double movement_x_power = (relative_x_to_point / (relative_abs_y+relative_abs_x)) * movement_speed;
        double movement_y_power = (relative_y_to_point / (relative_abs_y+relative_abs_x)) * movement_speed;

        //every movement has two states, the fast "gunning" section and the slow refining part. turn this var off when close to target

        double radToTarget = AngleWrap(point_angle - scaledWorldHeadingRad);
        double turnPower = 0;

        if(ourTurnStates == turnStates.speed) {
            if(Math.abs(radToTarget) > toDegrees(10)) {
                turnPower = radToTarget > 0 ? point_speed : -point_speed;
            }

            else {
                ourTurnStates = turnStates.accurate;
            }
        }

        if(ourTurnStates == turnStates.accurate) {
                turnPower = point_speed * radToTarget / toRadians(10);
                turnPower = Range.clip(turnPower, -point_speed, point_speed);
        }


        movementTurn = turnPower;
        movementX = movement_x_power;
        movementY = movement_y_power;


    }



    public static void pointAngle(double pointAngle, double pointSpeed, double deccelRad) {
        double relativePointAngle = AngleWrap(pointAngle - scaledWorldHeadingRad);

        double turnSpeed = (relativePointAngle/deccelRad) * pointSpeed;

        movementTurn = Range.clip(turnSpeed, -pointSpeed, pointSpeed);
        movementTurn = Range.clip(Math.abs(relativePointAngle)/ Math.toRadians(3), 0,1);
    }



    public static void gunToPosition(double targetX, double targetY, double movementSpeed, double pointAngle, double pointSpeed, double slowDownTurnRad, double slowDownMovementFromTurnErrorMax, boolean stop ) {


        double distanceToTarget = Math.hypot(scaledWorldXPos - targetX, scaledWorldYPos - targetY);


        double angleToTarget = atan2(targetY - scaledWorldYPos, targetX - scaledWorldXPos);
        double relativeAngleToTarget = AngleWrap(angleToTarget - scaledWorldHeadingRad);


        // angle correction rel x and y
        double relativeXToPoint = Math.cos(relativeAngleToTarget) * distanceToTarget;
        double relativeYToPoint = sin(relativeAngleToTarget) * distanceToTarget;

        double relativeAbsX = Math.abs(relativeXToPoint);
        double relativeAbsY = Math.abs(relativeYToPoint);



        /* motor powers */

        //let's initialize to a power that doesn't care how far we are away from the point
        //We do this by just calculating the ratios (shape) of the movement with respect to
        //the sum of the two components, (sum of the absolute values to preserve the sines)
        //so total magnitude should always equal 1
        double movementXComponent = (relativeXToPoint / (relativeAbsX + relativeAbsY));
        double movementYComponent = (relativeYToPoint / (relativeAbsX + relativeAbsY));


        //So we will basically not care about what movement_speed was given, we are going to
        //decelerate over the course of 30 cm anyways (100% to 0) and then clip the final values
        //to have a max of movement_speed.
        if (stop) {
            movementXComponent = relativeAbsX / 30.0;
            movementYComponent = relativeAbsY / 30.0;
        }

        movementX = Range.clip(movementXComponent, -movementSpeed, movementSpeed);
        movementY = Range.clip(movementYComponent, -movementSpeed, movementSpeed);


        // turn stuff here
        double absolutePointAngle = pointAngle + angleToTarget;


        double relativePointAngle = AngleWrap(absolutePointAngle - scaledWorldHeadingRad);


        double deccelDistance = Math.toRadians(40);

        // scale down angle by 40 and multiply by point speed
        double turnSpeed = (relativePointAngle / deccelDistance) * pointSpeed;


        movementTurn = Range.clip(turnSpeed, -pointSpeed, pointSpeed);
        if (distanceToTarget < 5) {
            movementTurn = 0;
        }


        // add smoothing near last 3 cm (no oscillate)
        movementX = Range.clip(relativeAbsX / 6, 0, 1);
        movementY = Range.clip(relativeAbsY / 6, 0, 1);

        movementTurn = Range.clip(Math.abs(relativePointAngle) / Math.toRadians(2), 0, 1);


        // slow down if heading off
        double turnErrorSoSlowDown = Range.clip(1.0 - Math.abs(relativePointAngle / slowDownTurnRad), 1.0 - slowDownMovementFromTurnErrorMax, 1);

        if (Math.abs(movementTurn) < 0.0001) {
            turnErrorSoSlowDown = 1;
        }


        movementX *= turnErrorSoSlowDown;
        movementY *= turnErrorSoSlowDown;

        target = new movementTarget(targetX, targetY, pointAngle);
    }


    public static movementTarget target;


    public static void followCurve(ArrayList<WayPoint> wayPoints) {
        WayPoint targetPoint = wayPoints.get(1);

        gunToPosition(targetPoint.targetX, targetPoint.targetY, targetPoint.movementSpeed, targetPoint.pointAngle, targetPoint.pointSpeed, targetPoint.slowDownRadians, targetPoint.slowDownErrorMax, false);

    }


    private static WayPoint getTargetPoint(ArrayList<WayPoint> wayPoints, movementTarget target) {
        for(int i=0; i < wayPoints.size() - 1; i++) {
            if(wayPoints.get(i).getMovementTarget().equals())
        }
    }



    public static void stopMovement() {
        movementX = 0;
        movementY = 0;
        movementTurn = 0;
    }
}