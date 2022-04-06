package upmc.akka.leader
import akka.actor._
import javax.sound.midi._
import javax.sound.midi.ShortMessage._

import scala.util.Random

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import scala.collection.immutable.ListMap
//import Musician._


class Manager (val id:Int, val terminaux:List[Terminal]) extends Actor {
    var activeCollegues:ListMap[Int,Int] = ListMap()
    val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor") 
    val TIME_BASE = 50 milliseconds 
    val PRINT_TIME_BASE = 500 milliseconds
    val PRINTER_TIME_BASE = 1500 milliseconds
    val COUNT_TIME_BASE = 100 milliseconds
    val INIT_TIME_BASE = 100 milliseconds
    val scheduler = context.system.scheduler
    val random = new Random

    var chief = -1
    var chiefage = -1
    var count = 0
    var lcount = 0
    var killme = true
    def receive = {

        case InitLooper =>{
           
           
            if(activeCollegues.size > 2){
                terminaux.foreach(n => {
                    if (n.id == id) {
                        
                        val remote = context.actorSelection("akka.tcp://MozartSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Musicien"+n.id)
                        remote ! Init
                    }
                
                })
            }  
            else{ scheduler.scheduleOnce(INIT_TIME_BASE, self, InitLooper)}
            
            
        
           
        }
        case Start =>{
               self ! Update
               self ! Looper
               self ! Counter
               self ! Printer
               self ! InitLooper
        }

        case Update =>{
         
            
            terminaux.foreach(n => {
                    if (n.id != id) {
                        
                        val remote = context.actorSelection("akka.tcp://MozartSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Musicien"+n.id)
                        remote ! Ping(id, count)
                    }
                
            })
            scheduler.scheduleOnce(TIME_BASE, self, Update)

        }

        case Ping(n, m)=>{
            
            if(chief == n) {chiefage = m}
            if(!activeCollegues.contains(n)){
                this.activeCollegues = this.activeCollegues ++ ListMap(n -> m)
                if(n != id){killme = false}
                
            }

        }

        case Looper =>{
            
         
            val tempList = activeCollegues ++ ListMap(id -> count)
            if(!tempList.contains(chief)){
                ListMap(tempList.toSeq.sortBy(_._1):_*)
                
                var newn = -1
                var newm = -1
                tempList.keys.foreach{ i =>
                    if(tempList(i) > newm){
                        newn = i
                        newm = tempList(i)
                    }
                }
                
                chief = newn
                chiefage = newm
            }

            
            terminaux.foreach(n => {
                    if (n.id != id) {
                        
                        val remote = context.actorSelection("akka.tcp://MozartSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Musicien"+n.id)
                        remote ! Leader(chief, chiefage)
                    }
                
            })
            if((count%3) == 0){
                var s = "MUSICIENS: ["
            
                val tempList = activeCollegues ++ ListMap(id -> count)
                
                ListMap(tempList.toSeq.sortBy(_._1):_*)
                
                for( i <- 0 to 3){
                if(tempList.contains(i)){s+="   "+i}
                }
            
                s+="  ]" 
                
                
                displayActor ! Message (s)
                displayActor ! Message ("LEADER: ( "+chief+" )")
            }
            this.activeCollegues = ListMap()

            

            scheduler.scheduleOnce(PRINT_TIME_BASE, self, Looper)
        }

        case IsLeader(n:Int) =>{
               sender ! IsLeader(chief)
        }

        case Leader(n, m) => {
      
            if(m>chiefage){
                chief = n
                chiefage = m
            }
            //sender ! Leader(chief, chiefage)
        }
        case GetPlayer(n) =>{
            val keys = activeCollegues.keys.toList
            if(activeCollegues.size != 0){sender ! GetPlayer(keys(random.nextInt(activeCollegues.size)))}
            
        }

        case Counter =>{
            count = count + 1
            if(id == chief){chiefage = count}
            if(id == chief){
              
                lcount = lcount +1
                if((lcount%200) == 0 ) {
                    if(killme){
                        displayActor ! Message ("NO ONE CAME :'( ")
                        System.exit(0)
                    }
                    
                    
                }
                if(killme == false){lcount = 0}
                killme = true
            }
            scheduler.scheduleOnce(COUNT_TIME_BASE, self, Counter)
        }
        case Printer =>{
            /*
            var s = "MUSICIENS: ["
        
            val tempList = activeCollegues ++ ListMap(id -> count)
            
            ListMap(tempList.toSeq.sortBy(_._1):_*)
            
            for( i <- 0 to 3){
               if(tempList.contains(i)){s+="   "+i}
            }
        
            s+="  ]" 
              
            
            displayActor ! Message (s)

            
            displayActor ! Message ("LEADER: ( "+chief+" )")
            scheduler.scheduleOnce(PRINTER_TIME_BASE, self, Printer)
            */
        }
        
    }
}