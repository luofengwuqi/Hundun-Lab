import request from '@/utils/request.js'

export const loginByUsernameAndPassword = async (username , password)=> {
  return await request.post(
     '/user/login/username-password',
     {
      username: username,
      password: password
    }
  )
}

export const loginByPhoneAndPassword = async (phone , password)=>{
  return await  request.post('/user/login/phone-password',
    {
    phone,
    password,
  })
}



export const logoutAPI = async ()=>{
  return await request.post('/user/login/logout')
}

export const addUser = async (user)=>{
  return await request.post('/user/register',user)
}

export const sendEmailCode = async (email, skipDefaultErrorHandling = false) =>{
  return await request.post('/user/login/email-code/send',{
    email:email
  }, {
    skipDefaultErrorHandling
  })
}

export const loginByEmailAndCode = async (email,code)=>{
  return await request.post('/user/login/email-code',{
    email,
    code
  })
}

export const loginByEmailAndPassword = async (email,password)=>{
  return await request.post('/user/login/email-password',{
    email,
    password
  })
}

export const updateAvatarAPI = async (formData) => {
  return await request.put('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getUserInfoAPI = async () => {
  return await request.get('/user/info')
}



export const updateNicknameAPI = async (huh,password) => {
  return await request.put('/user/nickname', {
    huh,
    password
  })
}

export const updateEmailAPI = async (huh,password) => {
  return await request.put('/user/email', {
    huh,
    password
  })
}

export const updatePhoneAPI = async (huh,password) => {
  return await request.put('/user/phone', {
    huh,
    password
  })
}

export const updatePasswordAPI = async (code,password) => {
  return await request.put('/user/password', {
    code,
    password
  })
}


