#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <errno.h>
#include <pthread.h>

#include "ptpHwP1000LinuxDr.h"
#include "type_defs.h"
#include "scheduler.h"

void CLOCK_init(CLOCK* clock, void* actual_ref, SCHEDULER* scheduler,
	void* method_table)
{
	INIT_SUPER_TYPE(CLOCK, ACTOR, clock, actual_ref, scheduler, method_table);
	COPY_METHOD_TABLE(CLOCK, clock, clock->super_fire,
		((ACTOR*) SUPER(clock))->fire);

	ACTOR* ACTOR_super = UPCAST(clock, ACTOR);
	TYPED_PORT_init(&(clock->fire_at), &(clock->fire_at), ACTOR_super);
	TYPED_PORT_init(&(clock->output), &(clock->output), ACTOR_super);
	clock->time = clock->end_time = (TIME) {
		0,	// ms
		0,	// ns
	};
	
	if (method_table == NULL)
		clock->fire = CLOCK_fire;
	else
		COPY_METHOD_TABLE(CLOCK, clock, clock->fire, method_table);
}

void CLOCK_fire(CLOCK* clock)
{
	clock->super_fire((ACTOR*) SUPER(clock));
}

void TRIGGERED_CLOCK_init(TRIGGERED_CLOCK* triggered_clock,
	void* actual_ref, SCHEDULER* scheduler, void* method_table)
{
	INIT_SUPER_TYPE(TRIGGERED_CLOCK, CLOCK, triggered_clock, actual_ref,
		scheduler, method_table);
	COPY_METHOD_TABLE(TRIGGERED_CLOCK, triggered_clock,
		triggered_clock->super_fire, ((CLOCK*) SUPER(triggered_clock))->fire);
	
	ACTOR* ACTOR_super = UPCAST(triggered_clock, ACTOR);
	TYPED_PORT_init(&(triggered_clock->trigger), &(triggered_clock->trigger),
		ACTOR_super);
	TYPED_PORT_init(&(triggered_clock->output), &(triggered_clock->output),
		ACTOR_super);
	triggered_clock->start_time = triggered_clock->phase
			= triggered_clock->period = (TIME) {
		0,	// ms
		0	// ns
	};
	
	if (method_table == NULL)
		triggered_clock->fire = TRIGGERED_CLOCK_fire;
	else
		COPY_METHOD_TABLE(CLOCK, triggered_clock, triggered_clock->fire,
			method_table);
}

void TRIGGERED_CLOCK_initialize(TRIGGERED_CLOCK* triggered_clock)
{
	int fd;
	char *devFile = "/dev/ptpHwP1000LinuxDr";
	SCHEDULER* scheduler;
    FPGA_GET_TIME fpgaGetTime;
    int rtn;
    unsigned int secs;
    unsigned int nsecs;
	
	scheduler = UPCAST(triggered_clock, ACTOR)->scheduler;
	SCHEDULER_register_port(scheduler,
		UPCAST(&(triggered_clock->trigger), PORT));
		
	fd = open(devFile, O_RDWR);
	if (fd < 0) {
		fprintf(stderr, "Error opening device file \"%s\"\n", devFile);
		exit(1);
	}
	scheduler->fd = fd;
	
    // Read the current time from the IEEE1588 clock
    rtn = ioctl(fd,  FPGA_IOC_GET_TIME, &fpgaGetTime);
    if (rtn) {
		fprintf(stderr, "ioctl to get time failed: %d, %d\n", rtn, errno);
		exit(1);
	}

    // Scale from HW to TAI nsec
    decodeHwNsec(&fpgaGetTime.timeVal, &secs, &nsecs);

	triggered_clock->start_time = (TIME) { secs, nsecs };
	UPCAST(triggered_clock, CLOCK)->end_time.ms +=
			triggered_clock->start_time.ms;

	INT_TOKEN token;
	INT_TOKEN_init(&token, &token);
	token.value = 1;
	
	CLOCK* triggered_clock_CLOCK = UPCAST(triggered_clock, CLOCK);
	triggered_clock_CLOCK->time = (TIME) {
		triggered_clock->start_time.ms + triggered_clock->phase.ms,
		triggered_clock->start_time.ns + triggered_clock->phase.ns
	};
	EVENT e = (EVENT) {
		UPCAST(&token, TOKEN),			// token
		triggered_clock_CLOCK->time,	// time
		0,								// is_timer_event
		NULL,							// prev
		NULL							// next
	};
	TYPED_PORT_send(&(triggered_clock->output), &e);
}

void TRIGGERED_CLOCK_fire(TRIGGERED_CLOCK* triggered_clock)
{
	triggered_clock->super_fire((CLOCK*) SUPER(triggered_clock));
}

void TRIGGER_OUT_init(TRIGGER_OUT* trigger_out, void* actual_ref,
	SCHEDULER* scheduler, void* method_table)
{
	INIT_SUPER_TYPE(TRIGGER_OUT, ACTOR, trigger_out, actual_ref, scheduler,
		method_table);
	COPY_METHOD_TABLE(TRIGGER_OUT, trigger_out, trigger_out->super_fire,
		((CLOCK*) SUPER(trigger_out))->fire);
	
	ACTOR* ACTOR_super = UPCAST(trigger_out, ACTOR);
	TYPED_PORT_init(&(trigger_out->input), &(trigger_out->input), ACTOR_super);
	TYPED_PORT_init(&(trigger_out->output), &(trigger_out->output),
		ACTOR_super);
	
	if (method_table == NULL)
		trigger_out->fire = TRIGGER_OUT_fire;
	else
		COPY_METHOD_TABLE(TRIGGER_OUT, trigger_out, trigger_out->fire,
			method_table);
}

void TRIGGER_OUT_fire(TRIGGER_OUT* trigger_out)
{
	trigger_out->super_fire((CLOCK*) SUPER(trigger_out));
}

int fd;

void* read_loop2(void* data)
{
	unsigned int secs;
	unsigned int nsecs;
	int rtn;
	TIME t;
	char* data1 = "X";
	
	unsigned int status;
	int num;

	do {
		// Block until the next interrupt
		do {
			num = read(fd, &status, sizeof(status));
			if (num != sizeof( status)) {
				fprintf(stderr, "Error reading status, %d\n", num);
				exit(1);
			}
		} while ((status & TIMEBOMB_0_FIRE) == 0); // Got it!
		//} while ((status & TIMESTAMP_0_RCV) == 0); // Got it!

		FPGA_GET_TIME fpgaGetTime;
		rtn = ioctl(fd,  FPGA_IOC_GET_TIME, &fpgaGetTime);
		if (rtn) {
			fprintf(stderr, "ioctl to get time failed: %d, %d\n", rtn, errno);
			exit(1);
		}

		// Scale from HW to TAI nsec
		decodeHwNsec( &fpgaGetTime.timeVal, &secs, &nsecs);
		printf("  sw TO: %.9d.%9.9d\n", secs, nsecs);
		//printf("\n%s>\n", (char *)data1);
		
		t.ms = secs; 
		t.ns = nsecs;
		INT_TOKEN token;
		INT_TOKEN_init(&token, &token);
		token.value = 1;
		EVENT e2 = (EVENT) {
			UPCAST(&token, TOKEN),			// token
			t,								// time
			0,								// is_timer_event
			NULL,							// prev
			NULL							// next
		};
		TYPED_PORT_send((TYPED_PORT*) data, &e2);
	} while (1);

	pthread_exit(NULL);
}

void TRIGGER_OUT_initialize(TRIGGER_OUT* trigger_out)
{
	int        thr_id;
	pthread_t  p_thread;
	SCHEDULER* scheduler;
	char *hostName = "X";
	
	scheduler = UPCAST(trigger_out, ACTOR)->scheduler;
	SCHEDULER_register_port(scheduler, UPCAST(&(trigger_out->input), PORT));
	
	//FIXME: create thread here...
    thr_id = pthread_create(&p_thread, NULL, read_loop2,
    	(void*)UPCAST(&(trigger_out->output), PORT));
}

int main() {
	SCHEDULER scheduler;
	SCHEDULER_init(&scheduler, &scheduler);
	
	TRIGGERED_CLOCK t_clock;
	TRIGGERED_CLOCK_init(&t_clock, &t_clock, &scheduler, NULL);
	TRIGGER_OUT t_out;
	TRIGGER_OUT_init(&t_out, &t_out, &scheduler, NULL);
	
	PORT_connect(UPCAST(&(t_clock.output), PORT), UPCAST(&(t_out.input), PORT));
	PORT_connect(UPCAST(&(t_out.output), PORT),
		UPCAST(&(t_clock.trigger), PORT));
	
	UPCAST(&t_clock, CLOCK)->end_time = (TIME) {50, 0};
	t_clock.period = (TIME) {5, 0};
	t_clock.phase = (TIME) {1, 0};
	TRIGGERED_CLOCK_initialize(&t_clock);
	TRIGGER_OUT_initialize(&t_out);
	
	printf("Start execution:");
	SCHEDULER_execute(&scheduler);
}
