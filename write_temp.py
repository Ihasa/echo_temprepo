#!/usr/bin/env python
import time
import os
import datetime
import locale
import RPi.GPIO as GPIO

GPIO.setmode(GPIO.BCM)
#=============config==============
DEBUG = 0 
MONITOR = 1 
LOGING = 1
SPAN = 5
#=================================

# read SPI data from MCP3008 chip, 8 possible adc's (0 thru 7)
def readadc(adcnum, clockpin, mosipin, misopin, cspin):
        if ((adcnum > 7) or (adcnum < 0)):
                return -1
        GPIO.output(cspin, True)

        GPIO.output(clockpin, False)  # start clock low
        GPIO.output(cspin, False)     # bring CS low

        commandout = adcnum
        commandout |= 0x18  # start bit + single-ended bit
        commandout <<= 3    # we only need to send 5 bits here
        for i in range(5):
                if (commandout & 0x80):
                        GPIO.output(mosipin, True)
                else:
                        GPIO.output(mosipin, False)
                commandout <<= 1
                GPIO.output(clockpin, True)
                GPIO.output(clockpin, False)

        adcout = 0
        # read in one empty bit, one null bit and 10 ADC bits
        for i in range(12):
                GPIO.output(clockpin, True)
                GPIO.output(clockpin, False)
                adcout <<= 1
                if (GPIO.input(misopin)):
                        adcout |= 0x1

        GPIO.output(cspin, True)
        
        adcout >>= 1       # first bit is 'null' so drop it
        return adcout

# change these as desired - they're the pins connected from the
# SPI port on the ADC to the Cobbler
#SPICLK = 18
#SPIMISO = 23
#SPIMOSI = 24
#SPICS = 25
# Above pin number seems not working. I modified the pin as below.
# See also: http://elinux.org/RPi_Low-level_peripherals
SPICLK = 11
SPIMISO = 9
SPIMOSI = 10
SPICS = 8
MOTION = 17
# set up the SPI interface pins
GPIO.setup(SPIMOSI, GPIO.OUT)
GPIO.setup(SPIMISO, GPIO.IN)
GPIO.setup(SPICLK, GPIO.OUT)
GPIO.setup(SPICS, GPIO.OUT)
GPIO.setup(MOTION, GPIO.IN)

# 10k trim pot connected to adc #0
potentiometer_adc = 0;
potentiometer_adc2=4;

potentiometer_adc3=5;
potentiometer_adc4=6;

last_read = 0       # this keeps track of the last potentiometer value
tolerance = 5       # to keep from being jittery we'll only change
                    # volume when the pot has moved more than 5 'counts'

while True:
        # we'll assume that the pot didn't move
        trim_pot_changed = False

        # read the analog pin
        t1 = readadc(potentiometer_adc, SPICLK, SPIMOSI, SPIMISO, SPICS)
        # how much has it changed since the last read?
        t2 = readadc(potentiometer_adc2, SPICLK, SPIMOSI, SPIMISO, SPICS)
        t3 = readadc(potentiometer_adc3, SPICLK, SPIMOSI, SPIMISO, SPICS)
        t4 = readadc(potentiometer_adc4, SPICLK, SPIMOSI, SPIMISO, SPICS)
       	print("t1:{0}". format(t1 * 330 / 1024.0))	
       	print("t2:{0}". format(t2 * 330 / 1024.0))	
       	print("t3:{0}". format(t3 * 330 / 1024.0))	
       	print("t4:{0}". format(t4 * 330 / 1024.0))	
	trim_pot = (t1 + t2 + t3 + t4 ) / 4.0 

	pot_adjust = abs(trim_pot - last_read)

        if DEBUG:
                print "trim_pot:", trim_pot
                print "pot_adjust:", pot_adjust
                print "last_read", last_read

        if ( pot_adjust > tolerance ):
               trim_pot_changed = True

        if DEBUG:
                print "trim_pot_changed", trim_pot_changed

        if ( trim_pot_changed ):
                # convert 12bit adc0 (0-4096) trim pot read into 0-100 volume level
                set_volume = trim_pot / (10.24 * 4)     

                set_volume = round(set_volume)          # round out decimal value
                set_volume = int(set_volume)            # cast volume as integer

                print 'Volume = {volume}%' .format(volume = set_volume)
                set_vol_cmd = 'sudo amixer cset numid=1 -- {volume}% > /dev/null' .format(volume = set_volume)
                os.system(set_vol_cmd)  # set volume

                if DEBUG:
                        print "set_volume", set_volume
                        print "tri_pot_changed", set_volume

                # save the potentiometer reading for the next loop
                last_read = trim_pot

        # hang out and do nothing for a half second
	"""////////////////////////////////////////////////////insert prog////////////////////////////////////////////////////////"""
	BUTTON = "nice"
	if GPIO.input(MOTION):
		motion = "true"
	else:
		motion = "false"


	date = datetime.datetime.now().strftime(u'%Y/%m/%d %H:%M:%S')
	if MONITOR:
		print ("xxxxxxxxxxxx____senssor_value____xxxxxxxxxxxxxxxxx")
		print (""),date
	temp = (trim_pot)*330/1024
	temp = (int)(temp * 10)
	
	if MONITOR:
		print (">>temp:{0}".format(temp))
		print (">>motion:{0}".format(motion))
		print ("xxxxxxxxxxxxxxxxxxxxxxxxxxxxx")

	#data_set=["<{0}>\n".format(date), "temp:{0}\n".format(temp), "motion:{0}\n".format(motion), "button:{0}\n".format(BUTTON), "\n"]
	data_set=["{0}".format(temp),"\n"]
	if LOGING:
		fp = open("./log.txt", 'a')
		fp.writelines(data_set)
		fp.close()

	fp = open("temperature.txt", 'w')
	fp.writelines(data_set)
	fp.close()
	"""///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////"""
        time.sleep(SPAN)
