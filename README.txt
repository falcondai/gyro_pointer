Project Name:
Gyro_pointer

Description:
Control a computer's cursor with a phone's movement.

Initiator:
Falcon Dai

Initial Date:
5/10/2012

Design Principles (and rationale):
0. fast and smooth response (basic for input devices).
1. place user interactions as much as we can on the phone (so a user doesn't need to get up to the computer).
2. security (controling/disturbing the cursor is devastating/annoying).
3. portability (the server side should support different OS's).

Glossary:
client: the app on the phone, the cursor controller
server: the daemon on the computer, the controlled
gyroscope:
rotation vector:

Communication Protocol:
Network
server UDP socket bound to port 50003

Message Structure:
20 bytes
8 bytes timestamp in long type
12 bytes payload

utilizing the fact that for a rotation vector (x sin(theta/2), y sin(theta/2), z sin(theta/2)), where (x, y, z) is a unit vecotr, each component is in [-1.0, 1.0], we can compactly encode other messages into the 12 bytes payload by avoiding the space taken up by rotation vectors.

Message Types and Payload Design:
	Rotation vector state: float x, float y, float z (each 4 bytes)
	Mouse button event: float type = 2.0f (0x00, 0x00, 0x00, 0x40), int button (0 left, 1 middle, 2 right), int state (>0 UP otherwise DOWN)
	

Gesture Design:
Pointer-like mapping of orientation to cursor position.

Message Encrytion Scheme:
