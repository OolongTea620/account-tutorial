## Account 프로젝트 실습

### 프로젝트 설정 

#### Redis 
**사용 이유**
    
    SpinLock을 활용한 동시성 제어
    동시성 제어를 AOP를 활용하여 실습하는데 활용되는 인프라

LocalRedis 실행 설정
SpringBoot가 기동하면서 Bean을 등록할 때 레디스를 실행하고,    
종료되면서 Bean을 삭제할 때 레디스를 종료하도록 설정

해당 Bean이의 패키지 순서를 윗쪽으로 해야한다 (Redis Repository 보다 빨리 뜰 수 있도록)
