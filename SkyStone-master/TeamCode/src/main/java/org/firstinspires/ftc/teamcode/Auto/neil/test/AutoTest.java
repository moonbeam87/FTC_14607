package org.firstinspires.ftc.teamcode.Auto.neil.test;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.Auto.neil.BaseAuto;


/**
 * extending base auto for localization methods
 */
@TeleOp
public class AutoTest extends BaseAuto {

    @Override
    public void runOpMode() {





        while(!isStarted()) {

        }

        phoneCam.closeCameraDevice();



        subMethod sMethod = new subMethod(STATUS.firstMovement) {
            @Override
            protected void method() {
                moveSlideOrSomething();
            }
        };





        verticalMovement(48, 0.5, 0.25, 4, 4, 0.75, Math.toRadians(20), STATUS.firstMovement);

    }


    private void moveSlideOrSomething() {
        // do something
    }
}
