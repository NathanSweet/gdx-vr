package com.badlogic.gdx.vr;

import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.HmdMatrix44;
import org.lwjgl.openvr.VRSystem;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.vr.VRContext.Eye;
import com.badlogic.gdx.vr.VRContext.Space;
import com.badlogic.gdx.vr.VRContext.VRDevice;
import com.badlogic.gdx.vr.VRContext.VRDeviceType;

/**
 * A {@link Camera} implementation for one {@link Eye}
 * of a {@link VRContext}. All properties except {@link Camera#near},
 * {@link Camera#far} and {@link #offset} will be overwritten
 * on every call to {@link #update()} based on the tracked values
 * from the head mounted display. The {@link #offset} 
 * vector allows you to position the camera in world space.
 * @author badlogic
 *
 */
public class VRCamera extends Camera {
	public final VRContext context;
	public final Eye eye;
	public final Matrix4 eyeSpace = new Matrix4();
	public final Matrix4 invEyeSpace = new Matrix4();	

	public VRCamera(VRContext context, Eye eye) {
		this.context = context;
		this.eye = eye;
	}
	
	@Override
	public void update() {
		update(true);		
	}
	
	HmdMatrix44 projectionMat = HmdMatrix44.create();
	HmdMatrix34 eyeMat = HmdMatrix34.create();

	@Override
	public void update(boolean updateFrustum) {
		// get the projection matrix from the HDM
		VRSystem.VRSystem_GetProjectionMatrix(eye.index, near, far, projectionMat);
		VRContext.hmdMat4toMatrix4(projectionMat, projection);
		
		// get the eye space matrix from the HDM
		VRSystem.VRSystem_GetEyeToHeadTransform(eye.index, eyeMat);
		VRContext.hmdMat34ToMatrix4(eyeMat, eyeSpace);
		invEyeSpace.set(eyeSpace).inv();
		 
		// get the pose matrix from the HDM
		VRDevice hmd = context.getDeviceByType(VRDeviceType.HeadMountedDisplay);
		view.idt();
		float[] m = view.val;
		Vector3 x = hmd.getRight(Space.World);
		Vector3 y = hmd.getUp(Space.World);
		Vector3 z = hmd.getDirection(Space.World);
		Vector3 p = hmd.getPosition(Space.World);
		m[Matrix4.M00] = x.x;
		m[Matrix4.M10] = x.y;
		m[Matrix4.M20] = x.z;
		
		m[Matrix4.M01] = y.x;
		m[Matrix4.M11] = y.y;
		m[Matrix4.M21] = y.z;
		
		m[Matrix4.M02] = -z.x;
		m[Matrix4.M12] = -z.y;
		m[Matrix4.M22] = -z.z;
		
		view.setTranslation(p);
		view.inv();
		
		position.set(p);
		direction.set(z);
		up.set(y);		
		
		combined.set(projection);
		Matrix4.mul(combined.val, invEyeSpace.val);
		Matrix4.mul(combined.val, view.val);		
		
		if (updateFrustum) {
			invProjectionView.set(combined);
			Matrix4.inv(invProjectionView.val);
			frustum.update(invProjectionView);
		}
	}	
}
