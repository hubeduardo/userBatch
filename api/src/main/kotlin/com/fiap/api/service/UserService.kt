package com.fiap.api.service

import com.fiap.api.domain.User
import com.fiap.api.entities.exception.UserException
import com.fiap.api.entities.request.CreateUserRequest
import com.fiap.api.entities.request.DeleteRequest
import com.fiap.api.entities.request.UpdateUserRequest
import com.fiap.api.entities.response.DeleteResponse
import com.fiap.api.repository.UserRepository
import com.fiap.api.utils.ErrorCode
import com.fiap.api.utils.SuccessCode
import com.fiap.api.utils.Translator
import com.fiap.api.utils.getError
import io.reactivex.Single
import io.reactivex.Single.just
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserService {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    @Autowired
    private lateinit var userRepository: UserRepository

    fun checkCreateUser(createUserRequest: CreateUserRequest): Single<User> {

        val users: MutableList<User> = userRepository.findByEmail(createUserRequest.email)

        return if (users.isEmpty()) {
            saveUser(User().convertToUser(createUserRequest))
        } else {
            Single.error(UserException("400", Translator.getMessage(ErrorCode.USER_ALREADY_EXIST)))
        }
    }

    fun checkUpdateUser(updateUserRequest: UpdateUserRequest): Single<User> {
        logger.info("Start checkUpdateUser by applicationUserId: ${updateUserRequest.id} with request: $updateUserRequest")

        return updateUser(updateUserRequest)
    }

    fun checkRemoveUser(deleteRequest: DeleteRequest, applicationUserId: String): Single<DeleteResponse> {
        logger.info("Start checkRemoveUser by applicationUserId: $applicationUserId with request: $deleteRequest")

        return removeUser(User(deleteRequest.id), applicationUserId)
    }

    fun updateUser(updateUserRequest: UpdateUserRequest): Single<User> {
        logger.info("Start updateUser by applicationUserId: ${updateUserRequest.id} with request: $updateUserRequest")

        return findById(updateUserRequest.id!!)
            .filter {
                it.isPresent
            }.flatMapSingle {
                save(User().mergeDataUser(updateUserRequest, it.get()))
            }.doOnSuccess {
                logger.info("End updateEvent by applicationUserId: $${updateUserRequest.id} with response: $it")
                logger.info("End updateEvent by applicationUserId: $${updateUserRequest.id} with request: $it to feed")
            }.doOnError {
                logger.error("Error updateEvent by applicationUserId: $${updateUserRequest.id} with error: ${it.getError()}")
            }.onErrorResumeNext {
                Single.error(UserException("400", Translator.getMessage(ErrorCode.USER_DOES_NOT_EXIST)))
            }
    }

    fun saveUser(user: User): Single<User> {
        logger.info("Start saveUser by applicationUserId: ${user.id} with request: $user")

        return save(user)
            .doOnSuccess {
                logger.info("End saveUser by applicationUserId: ${user.id} with response: $it")
                logger.info("End saveUser by applicationUserId: ${user.id} with request: $it to feed")
            }.doOnError {
                logger.error("Error saveUser by applicationUserId: ${user.id} with error: ${it.getError()}")
            }.onErrorResumeNext {
                Single.error(UserException("400", Translator.getMessage(ErrorCode.USER_TRY_AGAIN_LATER)))
            }
    }

    fun removeUser(user: User, applicationUserId: String): Single<DeleteResponse> {
        logger.info("Start removeEvent by applicationUserId: $applicationUserId with request: $user")

        return findById(user.id!!)
            .filter {
                it.isPresent
            }.flatMapSingle {
                remove(it.get()).map {
                    DeleteResponse()
                    .getDeleteUserResponse(user.id, Translator.getMessage(SuccessCode.USER_REMOVE))
                }
            }.doOnSuccess {
                logger.info("End removeUser by applicationUserId: $applicationUserId with response: $it")
                logger.info("End removeUser by applicationUserId: $applicationUserId with request: $it to feed")
            }.doOnError {
                logger.error("Error removeUser by applicationUserId: $applicationUserId with error: ${it.getError()}")
            }.onErrorResumeNext {
                Single.error(UserException("400", Translator.getMessage(ErrorCode.USER_DOES_NOT_EXIST)))
            }
    }

    fun findByDoc(doc: String) = just(userRepository.findByDoc(doc))

    fun findById(id: String) = just(userRepository.findById(id))

    private fun save(user: User) = just(userRepository.save(user))

    private fun remove(user: User) = just(userRepository.deleteById(user.id.toString()))
}
